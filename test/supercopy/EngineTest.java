package supercopy;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
public class EngineTest {
 static int checks;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 static void write(Path p,String s)throws Exception{Files.createDirectories(p.getParent());Files.write(p,s.getBytes("UTF-8"));}
 static String read(Path p)throws Exception{return new String(Files.readAllBytes(p),"UTF-8");}
 public static void main(String[] args)throws Exception {
  Path root=Paths.get(args[0]).resolve("engine-"+UUID.randomUUID());Files.createDirectories(root);Config c=new Config();c.source=root.resolve("source").toString();c.destination=root.resolve("dest").toString();Path s=Paths.get(c.source),d=Paths.get(c.destination);Files.createDirectories(s.resolve("vide"));write(s.resolve("sous/école.txt"),"alpha");write(s.resolve("zero"),"");
  CopyEngine.Result r=CopyEngine.copy(c,()->false,x->{});check(r.copied==2&&r.errors==0,"copie initiale");check(Files.isDirectory(d.resolve("vide")),"dossier vide");check(read(d.resolve("sous/école.txt")).equals("alpha"),"unicode");
  check(CopyEngine.copy(c,()->false,x->{}).copied==0,"idempotence");
  FileTime old=Files.getLastModifiedTime(s.resolve("sous/école.txt"));write(s.resolve("sous/école.txt"),"bravo");Files.setLastModifiedTime(s.resolve("sous/école.txt"),old);
  check(CopyEngine.copy(c,()->false,x->{}).copied==1,"contenu change a taille et date identiques");
  try(java.util.stream.Stream<Path> walk=Files.walk(d.resolve(CopyEngine.HISTORY))){check(walk.filter(Files::isRegularFile).anyMatch(p->{try{return read(p).equals("alpha");}catch(Exception e){return false;}}),"ancienne version conservee");}
  Files.delete(s.resolve("sous/école.txt"));write(d.resolve("destination-seule"),"garder");CopyEngine.copy(c,()->false,x->{});check(read(d.resolve("sous/école.txt")).equals("bravo")&&Files.exists(d.resolve("destination-seule")),"aucune propagation suppression");
  write(s.resolve("sous/école.txt"),"charlie");c.keepExisting=true;CopyEngine.copy(c,()->false,x->{});check(read(d.resolve("sous/école.txt")).equals("bravo"),"mode conserver");c.keepExisting=false;
  write(s.resolve("annule"),"a");CopyEngine.copy(c,()->true,x->{});check(!Files.exists(d.resolve("annule")),"annulation");
  write(s.resolve("conflit"),"fichier");Files.createDirectories(d.resolve("conflit"));check(CopyEngine.copy(c,()->false,x->{}).errors==1,"conflit dossier conserve");
  String dest=c.destination;c.destination=s.resolve("interdit").toString();try{CopyEngine.validate(c);throw new AssertionError("imbrication");}catch(java.io.IOException expected){checks++;}c.destination=c.source;try{CopyEngine.validate(c);throw new AssertionError("identique");}catch(java.io.IOException expected){checks++;}c.destination=dest;
  c.enabled=true;c.seconds=2;c.save(root.resolve("config"));Config reload=Config.load(root.resolve("config"));check(reload.enabled&&reload.source.equals(c.source)&&reload.seconds==2,"configuration");
  check(!Server.running(root.resolve("config")),"serveur absent");
  System.out.println("PASS : "+checks+" verifications. Donnees de test : "+root);
 }
}
