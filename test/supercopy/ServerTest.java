package supercopy;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class ServerTest {
 static void waitFile(Path p)throws Exception {for(int i=0;i<100&&!Files.exists(p);i++)Thread.sleep(100);if(!Files.exists(p))throw new AssertionError("Absent : "+p);}
 public static void main(String[] args)throws Exception {
  Path root=Paths.get(args[0]).toAbsolutePath().resolve("server-"+UUID.randomUUID());Files.createDirectories(root.resolve("source"));
  Config c=new Config();c.source=root.resolve("source").toString();c.destination=root.resolve("dest").toString();c.enabled=true;c.seconds=1;c.save(root.resolve("data"));
  String java=Paths.get(System.getProperty("java.home"),"bin","java.exe").toString();String cp=System.getProperty("java.class.path");
  Process server=new ProcessBuilder(java,"-Dsupercopy.home="+root.resolve("data"),"-cp",cp,"supercopy.Server").redirectErrorStream(true).redirectOutput(root.resolve("process.log").toFile()).start();
  try{
   Files.write(root.resolve("source/first.txt"),"one".getBytes("UTF-8"));waitFile(root.resolve("dest/first.txt"));
   if(!Server.running(root.resolve("data")))throw new AssertionError("Serveur absent");
   Process second=new ProcessBuilder(java,"-Dsupercopy.home="+root.resolve("data"),"-cp",cp,"supercopy.Server").start();
   if(!second.waitFor(5,TimeUnit.SECONDS)){second.destroy();throw new AssertionError("Double serveur");}
   Files.write(root.resolve("source/second.txt"),"two".getBytes("UTF-8"));waitFile(root.resolve("dest/second.txt"));
   c.enabled=false;c.save(root.resolve("data"));Thread.sleep(1600);Files.write(root.resolve("source/paused.txt"),new byte[]{1});Thread.sleep(1600);
   if(Files.exists(root.resolve("dest/paused.txt")))throw new AssertionError("Veille ignoree");
   c.enabled=true;c.save(root.resolve("data"));waitFile(root.resolve("dest/paused.txt"));
   System.out.println("PASS : serveur independant, cycles repetes, verrou unique, veille et reprise.");
  }finally{server.destroy();server.waitFor(5,TimeUnit.SECONDS);}
 }
}
