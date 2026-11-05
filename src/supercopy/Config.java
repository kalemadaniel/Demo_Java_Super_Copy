package supercopy;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class Config {
    public String source="", destination="";
    public int seconds=30;
    public boolean enabled=false, keepExisting=false, permissions=false;
    public static Path home() {
        String override=System.getProperty("supercopy.home");
        if(override!=null)return Paths.get(override).toAbsolutePath();
        try {return Paths.get(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().resolve("data");}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public static Config load(Path home) throws IOException {
        Config c=new Config(); Path f=home.resolve("config.properties");
        if(!Files.exists(f))return c;
        Properties p=new Properties();try(InputStream in=Files.newInputStream(f)){p.load(in);}
        c.source=p.getProperty("source","");c.destination=p.getProperty("destination","");
        c.seconds=Integer.parseInt(p.getProperty("seconds","30"));
        if(c.seconds<1||c.seconds>86400)throw new IOException("Intervalle invalide (1 à 86400 secondes).");
        c.enabled=Boolean.parseBoolean(p.getProperty("enabled"));
        c.keepExisting=Boolean.parseBoolean(p.getProperty("keepExisting"));
        c.permissions=Boolean.parseBoolean(p.getProperty("permissions"));return c;
    }
    public void save(Path home) throws IOException {
        if(seconds<1||seconds>86400)throw new IOException("Intervalle invalide.");
        Files.createDirectories(home);Properties p=new Properties();
        p.setProperty("source",source);p.setProperty("destination",destination);p.setProperty("seconds",""+seconds);
        p.setProperty("enabled",""+enabled);p.setProperty("keepExisting",""+keepExisting);p.setProperty("permissions",""+permissions);
        Path temp=Files.createTempFile(home,"config-",".tmp");
        try {try(OutputStream out=Files.newOutputStream(temp)){p.store(out,"Demo_Java_Super_Copy");} replace(temp,home.resolve("config.properties"));}
        finally {Files.deleteIfExists(temp);}
    }
    static void replace(Path a,Path b)throws IOException {
        try{Files.move(a,b,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException e){Files.move(a,b,StandardCopyOption.REPLACE_EXISTING);}
    }
}
