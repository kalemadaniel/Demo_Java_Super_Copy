package supercopy;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public final class Server {
    final Path home;volatile boolean stopping=false;long next=0;
    Server(Path p){home=p;}
    void log(String s){try{Files.write(home.resolve("journal-"+LocalDate.now()+".log"),(LocalTime.now().withNano(0)+"  "+s+System.lineSeparator()).getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException e){System.err.println(e);}}
    void status(String s){try{Path t=Files.createTempFile(home,"status-",".tmp");Files.write(t,(LocalDateTime.now().withNano(0)+" | "+s).getBytes("UTF-8"));Config.replace(t,home.resolve("status.txt"));}catch(IOException e){log("Statut : "+e.getMessage());}}
    public static boolean running(Path home){
        try{Files.createDirectories(home);try(FileChannel channel=FileChannel.open(home.resolve("server.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE)){try{FileLock l=channel.tryLock();if(l==null)return true;l.release();return false;}catch(OverlappingFileLockException e){return true;}}}catch(IOException e){throw new IllegalStateException("Impossible de vérifier le serveur : "+e.getMessage(),e);}
    }
    boolean cancelled(){try{return stopping||!Config.load(home).enabled;}catch(Exception e){return true;}}
    void run()throws Exception {
        Files.createDirectories(home);
        try(FileChannel channel=FileChannel.open(home.resolve("server.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=channel.tryLock()) {
            if(lock==null)return;
            Runtime.getRuntime().addShutdownHook(new Thread(()->stopping=true));log("Serveur démarré sous le compte "+System.getProperty("user.name"));
            boolean wasEnabled=false;String previous="";
            while(!stopping){
                try{
                    Config c=Config.load(home);String signature=c.source+"\n"+c.destination+"\n"+c.seconds+"\n"+c.keepExisting+"\n"+c.permissions;
                    if(!signature.equals(previous)){next=0;previous=signature;}
                    boolean immediate=Files.deleteIfExists(home.resolve("copy-now"));
                    if(c.enabled&&(!wasEnabled||immediate||System.currentTimeMillis()>=next)) {
                        status("Copie en cours");log("Début du cycle");
                        try{CopyEngine.Result r=CopyEngine.copy(c,this::cancelled,this::log);log("Fin : "+r);status((cancelled()?"Suspendu | ":"Dernier cycle | ")+r);}
                        catch(Exception e){log("Échec du cycle : "+e.getMessage());status("Erreur : "+e.getMessage());}
                        next=System.currentTimeMillis()+c.seconds*1000L;
                    }else if(!c.enabled)status("En veille — aucune copie programmée");
                    wasEnabled=c.enabled;
                }catch(Exception e){status("Configuration inaccessible : "+e.getMessage());log("Erreur : "+e.getMessage());}
                Thread.sleep(1000);
            }
        }
    }
    public static void main(String[] args)throws Exception{new Server(Config.home()).run();}
}
