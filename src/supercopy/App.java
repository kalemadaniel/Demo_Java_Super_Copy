package supercopy;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;

public final class App extends JFrame {
    final Path home=Config.home();
    final JTextField source=new JTextField(),destination=new JTextField();
    final JSpinner interval=new JSpinner(new SpinnerNumberModel(30,1,86400,1));
    final JCheckBox keep=new JCheckBox("Conserver les fichiers existants sans les mettre à jour"),acl=new JCheckBox("Reproduire les autorisations des fichiers (ACL / POSIX)");
    final JLabel state=new JLabel("Serveur : vérification…"),detail=new JLabel("Source → destination • aucune suppression"),srcState=new JLabel("Lecture requise"),dstState=new JLabel("Écriture requise");
    final JTextArea journal=new JTextArea();
    final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"super-copy-ui");t.setDaemon(true);return t;});
    final JButton save=new JButton("Enregistrer"),start=new JButton("Activer le serveur"),pause=new JButton("Mettre en veille"),now=new JButton("Copier maintenant"),test=new JButton("Tester les accès");
    boolean busy=false; final javax.swing.Timer timer;
    public App(){
        super("Demo_Java_Super_Copy");setDefaultCloseOperation(DISPOSE_ON_CLOSE);setSize(1120,800);setMinimumSize(new Dimension(960,700));setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout(0,18));root.setBackground(new Color(241,245,249));root.setBorder(new EmptyBorder(22,28,14,28));setContentPane(root);
        JPanel header=new JPanel(new GridLayout(2,1,0,7));header.setOpaque(false);JLabel title=new JLabel("Super Copy  /  Serveur de copie");title.setFont(new Font("Segoe UI",Font.BOLD,28));title.setForeground(new Color(24,42,65));header.add(title);header.add(new JLabel("Demo_Java_Super_Copy • Java Swing • Copies automatiques sans suppression"));root.add(header,BorderLayout.NORTH);
        JPanel main=new JPanel();main.setOpaque(false);main.setLayout(new BoxLayout(main,BoxLayout.Y_AXIS));root.add(main,BorderLayout.CENTER);
        JPanel locations=new JPanel(new GridLayout(1,2,18,0));locations.setOpaque(false);locations.add(location("EMPLACEMENT SOURCE",source,srcState));locations.add(location("EMPLACEMENT DESTINATION",destination,dstState));locations.setMaximumSize(new Dimension(Integer.MAX_VALUE,150));main.add(locations);main.add(Box.createVerticalStrut(14));
        JPanel settings=card();settings.setLayout(new BoxLayout(settings,BoxLayout.Y_AXIS));JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT));row.setOpaque(false);row.add(new JLabel("Intervalle (secondes) :"));row.add(interval);row.add(save);row.add(test);settings.add(row);keep.setOpaque(false);acl.setOpaque(false);settings.add(keep);settings.add(acl);settings.add(new JLabel("Par défaut : mise à jour avec conservation de l’ancienne version dans .super-copy-history."));main.add(settings);main.add(Box.createVerticalStrut(14));
        JPanel status=card();status.setLayout(new BorderLayout(0,12));state.setFont(new Font("Segoe UI",Font.BOLD,21));state.setForeground(new Color(36,99,235));status.add(state,BorderLayout.NORTH);status.add(detail,BorderLayout.CENTER);JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));actions.setOpaque(false);actions.add(start);actions.add(now);actions.add(pause);status.add(actions,BorderLayout.SOUTH);main.add(status);main.add(Box.createVerticalStrut(14));
        JPanel logs=card();logs.setLayout(new BorderLayout(0,8));logs.add(new JLabel("JOURNAL D’ACTIVITÉ"),BorderLayout.NORTH);journal.setEditable(false);journal.setFont(new Font("Consolas",Font.PLAIN,12));journal.setLineWrap(true);journal.setWrapStyleWord(true);logs.add(new JScrollPane(journal));logs.setPreferredSize(new Dimension(900,230));main.add(logs);
        JPanel footer=new JPanel(new GridLayout(2,1));footer.setOpaque(false);footer.add(new JLabel("Fermer cette fenêtre laisse le serveur actif. « Mettre en veille » suspend les copies.",SwingConstants.CENTER));footer.add(new JLabel("Développeur Kalema Daniel Jonathan, kalemadaniel2014@gmail.com.",SwingConstants.CENTER));root.add(footer,BorderLayout.SOUTH);
        try{Config c=Config.load(home);source.setText(c.source);destination.setText(c.destination);interval.setValue(c.seconds);keep.setSelected(c.keepExisting);acl.setSelected(c.permissions);}catch(Exception e){error(e);}
        save.addActionListener(e->{final Config c=read();execute(()->{c.enabled=Config.load(home).enabled;CopyEngine.validate(c);c.save(home);});});
        start.addActionListener(e->activate(false));now.addActionListener(e->activate(true));
        pause.addActionListener(e->execute(()->{Config c=Config.load(home);c.enabled=false;c.save(home);}));
        test.addActionListener(e->{Config c=read();execute(()->{Path[] p=CopyEngine.validate(c);try(DirectoryStream<Path> stream=Files.newDirectoryStream(p[0])){stream.iterator().hasNext();}Files.createDirectories(p[1]);Path probe=Files.createTempFile(p[1],".super-copy-access-",".tmp");Files.delete(probe);SwingUtilities.invokeLater(()->{srcState.setText("Dossier lisible (compte actuel)");dstState.setText("Écriture réussie (compte actuel)");JOptionPane.showMessageDialog(this,"Accès aux dossiers vérifié sous "+System.getProperty("user.name")+".\nLes droits de chaque fichier sont vérifiés pendant la copie.\nUn compte de démarrage Windows différent doit avoir ses propres accès.");});});});
        timer=new javax.swing.Timer(1500,e->refresh());timer.start();refresh();addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){timer.stop();worker.shutdown();}});
    }
    JPanel location(String name,JTextField field,JLabel label){JPanel p=card();p.setLayout(new BorderLayout(0,12));JLabel title=new JLabel(name);title.setFont(new Font("Segoe UI",Font.BOLD,16));p.add(title,BorderLayout.NORTH);JPanel row=new JPanel(new BorderLayout(6,0));row.setOpaque(false);row.add(field);JButton browse=new JButton("Parcourir…");row.add(browse,BorderLayout.EAST);p.add(row);p.add(label,BorderLayout.SOUTH);browse.addActionListener(e->{JFileChooser f=new JFileChooser();f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);if(!field.getText().trim().isEmpty())f.setSelectedFile(new File(field.getText()));if(f.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)field.setText(f.getSelectedFile().getAbsolutePath());});return p;}
    static JPanel card(){JPanel p=new JPanel();p.setBackground(Color.WHITE);p.setBorder(new CompoundBorder(new LineBorder(new Color(220,227,237)),new EmptyBorder(14,14,14,14)));return p;}
    Config read(){Config c=new Config();c.source=source.getText().trim();c.destination=destination.getText().trim();c.seconds=(Integer)interval.getValue();c.keepExisting=keep.isSelected();c.permissions=acl.isSelected();return c;}
    interface Work {void run()throws Exception;}
    void execute(Work task){if(busy)return;busy=true;buttons(false);worker.submit(()->{try{task.run();}catch(Exception e){SwingUtilities.invokeLater(()->error(e));}finally{SwingUtilities.invokeLater(()->{busy=false;buttons(true);refresh();});}});}
    void buttons(boolean enabled){for(JButton b:new JButton[]{save,start,pause,now,test})b.setEnabled(enabled);}
    void activate(boolean immediate){final Config c=read();execute(()->{CopyEngine.validate(c);c.enabled=true;c.save(home);if(immediate)Files.write(home.resolve("copy-now"),new byte[0]);if(!Server.running(home)){
        String java=Paths.get(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"javaw.exe":"java").toString();
        new ProcessBuilder(java,"-Dsupercopy.home="+home,"-cp",System.getProperty("java.class.path"),"supercopy.Server").redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(home.resolve("startup.log").toFile())).start();
        for(int i=0;i<30&&!Server.running(home);i++)Thread.sleep(100);
        if(!Server.running(home))throw new IOException("Le serveur n’a pas démarré. Consultez data/startup.log.");
    }});}
    void refresh(){if(busy)return;busy=true;worker.submit(()->{String s,d,l;try{boolean running=Server.running(home);Config c=Config.load(home);s=running?(c.enabled?"● Serveur actif":"● Serveur en veille"):"● Serveur arrêté";Path status=home.resolve("status.txt");d=Files.exists(status)?new String(Files.readAllBytes(status),"UTF-8"):"Prêt à configurer";Path log=home.resolve("journal-"+LocalDate.now()+".log");l=tail(log);}catch(Exception e){s="Erreur d’accès";d=e.getMessage();l="";}final String fs=s,fd=d,fl=l;SwingUtilities.invokeLater(()->{state.setText(fs);detail.setText(fd);if(!journal.getText().equals(fl)){journal.setText(fl);journal.setCaretPosition(journal.getDocument().getLength());}busy=false;});});}
    static String tail(Path p)throws IOException {if(!Files.exists(p))return "Bienvenue. Configurez les deux dossiers, testez les accès puis activez le serveur.";try(RandomAccessFile f=new RandomAccessFile(p.toFile(),"r")){long offset=Math.max(0,f.length()-48000);f.seek(offset);byte[] data=new byte[(int)(f.length()-offset)];f.readFully(data);String s=new String(data,"UTF-8");return offset>0?s.substring(Math.max(0,s.indexOf('\n')+1)):s;}}
    void error(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Super Copy — vérification nécessaire",JOptionPane.ERROR_MESSAGE);}
    public static void main(String[] args){SwingUtilities.invokeLater(()->{try{for(UIManager.LookAndFeelInfo i:UIManager.getInstalledLookAndFeels())if("Nimbus".equals(i.getName()))UIManager.setLookAndFeel(i.getClassName());}catch(Exception ignored){}new App().setVisible(true);});}
}
