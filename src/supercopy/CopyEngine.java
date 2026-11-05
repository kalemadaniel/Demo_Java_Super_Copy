package supercopy;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.*;

public final class CopyEngine {
    static final String HISTORY=".super-copy-history";
    public static final class Result {public int copied, skipped, errors; public String toString(){return copied+" copié(s), "+skipped+" inchangé(s)/conservé(s), "+errors+" erreur(s)";}}
    public static Path[] validate(Config c)throws IOException {
        if(c.source.trim().isEmpty()||c.destination.trim().isEmpty())throw new IOException("Indiquez la source et la destination.");
        Path rawSource=Paths.get(c.source).toAbsolutePath().normalize(),rawDest=Paths.get(c.destination).toAbsolutePath().normalize();
        noLinks(rawSource);noLinks(rawDest);
        Path src=rawSource.toFile().getCanonicalFile().toPath(),dst=resolveExisting(rawDest);
        if(!Files.isDirectory(src))throw new IOException("La source doit être un dossier accessible.");
        if(src.startsWith(dst)||dst.startsWith(src)||Files.exists(dst)&&Files.isSameFile(src,dst))throw new IOException("Les dossiers doivent être distincts et non imbriqués.");
        if(Files.exists(src.resolve(HISTORY),LinkOption.NOFOLLOW_LINKS))throw new IOException("Nom réservé dans la source : "+HISTORY);
        if(Files.exists(dst)&&!Files.isDirectory(dst))throw new IOException("La destination doit être un dossier.");
        return new Path[]{src,dst};
    }
    static Path resolveExisting(Path p)throws IOException {
        if(Files.exists(p,LinkOption.NOFOLLOW_LINKS))return p.toFile().getCanonicalFile().toPath();
        Path parent=p.getParent();if(parent==null)throw new IOException("Lecteur indisponible : "+p);
        return resolveExisting(parent).resolve(p.getFileName());
    }
    static void noLinks(Path p)throws IOException {
        for(Path q=p;q!=null;q=q.getParent())if(Files.exists(q,LinkOption.NOFOLLOW_LINKS)) {
            BasicFileAttributes a=Files.readAttributes(q,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
            if(a.isSymbolicLink()||a.isOther())throw new IOException("Lien ou jonction refusé : "+q);
        }
    }
    public static Result copy(Config c,BooleanSupplier cancel,Consumer<String> log)throws IOException {
        Path[] roots=validate(c);Path source=roots[0],dest=roots[1];Files.createDirectories(dest);Result r=new Result();
        Files.walkFileTree(source,new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes a)throws IOException {
                if(cancel.getAsBoolean())return FileVisitResult.TERMINATE;
                try{noLinks(dir);Path target=dest.resolve(source.relativize(dir));noLinks(target);Files.createDirectories(target);return FileVisitResult.CONTINUE;}
                catch(IOException ex){r.errors++;log.accept("Dossier ignoré : "+dir+" — "+ex.getMessage());return FileVisitResult.SKIP_SUBTREE;}
            }
            public FileVisitResult visitFile(Path file,BasicFileAttributes attrs) {
                if(cancel.getAsBoolean())return FileVisitResult.TERMINATE;
                Path temp=null;
                try {
                    if(!attrs.isRegularFile()){r.skipped++;log.accept("Lien ou fichier spécial ignoré : "+file);return FileVisitResult.CONTINUE;}
                    noLinks(file);Path target=dest.resolve(source.relativize(file));noLinks(target);
                    if(Files.exists(target)&&!Files.isRegularFile(target))throw new IOException("Conflit avec un dossier existant.");
                    if(Files.exists(target)&&(c.keepExisting||same(file,target))){r.skipped++;return FileVisitResult.CONTINUE;}
                    BasicFileAttributes before=Files.readAttributes(file,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
                    temp=Files.createTempFile(target.getParent(),".super-copy-",".part");
                    try(InputStream in=Files.newInputStream(file);OutputStream out=Files.newOutputStream(temp)) {
                        byte[] buf=new byte[1024*1024];int n;while((n=in.read(buf))!=-1){if(cancel.getAsBoolean())throw new IOException("Copie interrompue ; destination précédente conservée.");out.write(buf,0,n);}
                    }
                    BasicFileAttributes after=Files.readAttributes(file,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
                    if(before.size()!=after.size()||!before.lastModifiedTime().equals(after.lastModifiedTime())||!same(file,temp))throw new IOException("Source modifiée pendant la copie ; nouvel essai au prochain cycle.");
                    Files.setLastModifiedTime(temp,before.lastModifiedTime());
                    if(c.permissions)copyPermissions(file,temp);
                    noLinks(target);
                    if(Files.exists(target)) {
                        Path history=dest.resolve(HISTORY).resolve(UUID.randomUUID().toString()).resolve(source.relativize(file));
                        noLinks(history);Files.createDirectories(history.getParent());Files.copy(target,history,StandardCopyOption.COPY_ATTRIBUTES);
                        log.accept("Ancienne version conservée : "+history);
                    }
                    Config.replace(temp,target);temp=null;r.copied++;log.accept("Copié : "+source.relativize(file));
                } catch(Exception e){r.errors++;log.accept("Erreur : "+file+" — "+e.getMessage());}
                finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult visitFileFailed(Path p,IOException e){r.errors++;log.accept("Accès impossible : "+p+" — "+e.getMessage());return cancel.getAsBoolean()?FileVisitResult.TERMINATE:FileVisitResult.CONTINUE;}
        });return r;
    }
    static boolean same(Path a,Path b)throws IOException {
        if(Files.size(a)!=Files.size(b))return false;
        try(InputStream x=new BufferedInputStream(Files.newInputStream(a));InputStream y=new BufferedInputStream(Files.newInputStream(b))) {
            byte[] bx=new byte[65536],by=new byte[65536];int nx;
            while((nx=readBlock(x,bx))>0){int ny=readBlock(y,by);if(nx!=ny)return false;for(int i=0;i<nx;i++)if(bx[i]!=by[i])return false;}return y.read()==-1;
        }
    }
    static int readBlock(InputStream in,byte[] b)throws IOException {int n=0,k;while(n<b.length&&(k=in.read(b,n,b.length-n))!=-1)n+=k;return n;}
    static void copyPermissions(Path from,Path to)throws IOException {
        AclFileAttributeView a=Files.getFileAttributeView(from,AclFileAttributeView.class),b=Files.getFileAttributeView(to,AclFileAttributeView.class);
        if(a!=null&&b!=null){b.setAcl(a.getAcl());return;}
        PosixFileAttributeView p=Files.getFileAttributeView(from,PosixFileAttributeView.class),q=Files.getFileAttributeView(to,PosixFileAttributeView.class);
        if(p!=null&&q!=null){q.setPermissions(p.readAttributes().permissions());return;}
        throw new IOException("Copie des autorisations non prise en charge sur ce volume.");
    }
}
