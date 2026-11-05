package supercopy;
import java.awt.*;
import java.awt.image.*;
import java.nio.file.*;
import javax.swing.*;
import javax.imageio.*;
public class UiTest {
 public static void main(String[] args)throws Exception {
  System.setProperty("supercopy.home",Paths.get(args[0],"ui-data").toAbsolutePath().toString());
  SwingUtilities.invokeAndWait(()->{try{
   for(UIManager.LookAndFeelInfo i:UIManager.getInstalledLookAndFeels())if("Nimbus".equals(i.getName()))UIManager.setLookAndFeel(i.getClassName());
   App app=new App();app.addNotify();app.validate();Container content=app.getContentPane();BufferedImage image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();content.printAll(g);g.dispose();ImageIO.write(image,"png",Paths.get(args[0],"apercu.png").toFile());app.timer.stop();app.worker.shutdown();app.dispose();
  }catch(Exception e){throw new RuntimeException(e);}});System.out.println("PASS : interface Swing construite et rendue.");
 }
}
