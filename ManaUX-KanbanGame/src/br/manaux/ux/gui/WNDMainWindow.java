/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.manaux.ux.gui;

import br.manaux.model.bean.Postit;
import br.manaux.model.pojo.GameEvents;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author dmitry
 */
public class WNDMainWindow extends javax.swing.JFrame {

    private boolean handMoving = false;
    private boolean playing = false;
    private boolean handHidden = true;
    private boolean postitShowing = false;
    private boolean animating = false;
    private volatile int curLoc;
    private int money;
    private Postit currentPostit = null;
    private JLabel jLbRunningMoney;
    private final GameEvents gameEvents;
    private Point startRunningMoneyPos;
    private int yPos;
    private int toDo = 4;
    private int addictionalMoneyPerSecond = 0;

    private final Runnable runningOutOfMoney = new Runnable() {
        @Override
        public synchronized void run() {
            int moneyPs = currentPostit.getMoneyPerSecond();
            while (playing) {
                try {
                    money -= (moneyPs + addictionalMoneyPerSecond);
                    updateMoney();

                    jLbRunningMoney.setLocation(startRunningMoneyPos);
                    jLbRunningMoney.setText("- R$ " + (moneyPs + addictionalMoneyPerSecond) + ",00");

                    yPos = startRunningMoneyPos.y;
                    new Thread(() -> {
                        jLbRunningMoney.setVisible(true);
                        for (int i = 0; i < 20; ++i, --yPos) {
                            jLbRunningMoney.setLocation(startRunningMoneyPos.x, yPos);
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(WNDMainWindow.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        jLbRunningMoney.setVisible(false);
                    }).start();

                    wait(1800);
                } catch (InterruptedException ex) {
                    Logger.getLogger(WNDMainWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    };

    private final Runnable toggleHandRunnable = new Runnable() {
        @Override
        public synchronized void run() {
            int y = Toolkit.getDefaultToolkit().getScreenSize().height;

            new Thread(() -> {
                while (handMoving) {
                    jPpainelMao.setLocation(0, curLoc);
                }
            }).start();

            postitShowing = handHidden;
            jTpostitMoney.setText("R$ " + money + ",00");

            while (handMoving) {
                if (handHidden) {
                    for (curLoc = y; curLoc > 0; curLoc -= 8) {
                        try {
                            wait(0, 18);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(WNDMainWindow.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    for (curLoc = 0; curLoc < y; curLoc += 8) {
                        try {
                            wait(0, 18);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(WNDMainWindow.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                handMoving = false;
                handHidden = !handHidden;
                validate();
            }
        }
    };

    public enum LocationType {

        TODO, DOING, VALIDATION, DONE, FINISHED;

    }

    private final HashMap<LocationType, LocationType> nextLocationType = new HashMap<>();
    private final HashMap<LocationType, LocationType> lastLocationType = new HashMap<>();
    private final HashMap<LocationType, boolean[]> freeSpaceMap = new HashMap<>();
    private final HashMap<LocationType, JPanel> panelMap = new HashMap<>();

    public WNDMainWindow(int money) {
        initComponents();
        this.dispose();

        nextLocationType.put(LocationType.TODO, LocationType.DOING);
        nextLocationType.put(LocationType.DOING, LocationType.VALIDATION);
        nextLocationType.put(LocationType.VALIDATION, LocationType.DONE);
        nextLocationType.put(LocationType.DONE, LocationType.TODO);

        lastLocationType.put(LocationType.TODO, LocationType.DONE);
        lastLocationType.put(LocationType.DOING, LocationType.TODO);
        lastLocationType.put(LocationType.VALIDATION, LocationType.DOING);
        lastLocationType.put(LocationType.DONE, LocationType.VALIDATION);

        freeSpaceMap.put(LocationType.TODO, new boolean[12]);
        freeSpaceMap.put(LocationType.DOING, new boolean[12]);
        freeSpaceMap.put(LocationType.VALIDATION, new boolean[12]);
        freeSpaceMap.put(LocationType.DONE, new boolean[12]);

        panelMap.put(LocationType.TODO, jPpTodo);
        panelMap.put(LocationType.DOING, jPpdoing);
        panelMap.put(LocationType.VALIDATION, jPpvalidation);
        panelMap.put(LocationType.DONE, jPpdone);

        gameEvents = new GameEvents(this);
        this.money = money;
        updateMoney();

        this.setUndecorated(true);
        this.setLocationRelativeTo(null);
        this.setExtendedState(MAXIMIZED_BOTH);

        jPpTodo.setOpaque(false);
        jTpostitData.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        jPpdoing.setOpaque(false);
        jPpvalidation.setOpaque(false);
        jPpdone.setOpaque(false);
        this.setVisible(true);

        initBackgroundImage();
        initMoneyLabel();

        ArrayList<Integer> values = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        Collections.shuffle(values);
        Postit postit = loadFromFile("./activities/calculator_pt1.in", values.get(0), values.get(1));
        Postit postit2 = loadFromFile("./activities/calculator_pt2.in", values.get(2), values.get(3));
        Postit postit3 = loadFromFile("./activities/legacy.in", values.get(4), values.get(5));
        Postit postit4 = loadFromFile("./activities/epic_only.in", values.get(6), values.get(7));
//        Postit postit = new Postit(this, LocationType.TODO, "Test", "Só testando na brotheragem", 300, values.get(0), values.get(1));
//        Postit postit2 = new Postit(this, LocationType.TODO, "Aivey", "Só testando na brotheragem", 400, values.get(2), values.get(3));
//        Postit postit3 = new Postit(this, LocationType.TODO, "Oyvey", "Só testando na brotheragem", 500, values.get(4), values.get(5));
        jLbBackground.add(postit);
        jLbBackground.add(postit2);
        jLbBackground.add(postit3);
        jLbBackground.add(postit4);
    }


    /*
    * starts the bgimage
     */
    private Postit loadFromFile(String file, int doingTeam, int validationTeam) {
        Postit it = null;
        String title = null;
        StringBuilder content;
        int money = 0;

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            if (bufferedReader.ready()) {
                String[] line = bufferedReader.readLine().split(":");
                if (line.length > 1) {
                    title = line[0];
                    money = Integer.parseInt(line[1]);
                } else {
                    return null;
                }
            }
            content = new StringBuilder();
            content.append("<html><p align='center'>");
            while (bufferedReader.ready()) {
                content.append(bufferedReader.readLine().replace("<red>", "<font color='red'>").replace("<blue>", "<font color='blue'>")).append("<br>");
            }
            content.append("</p></html>");
            it = new Postit(this, LocationType.TODO, title, content.toString(), money, doingTeam, validationTeam, file.contains("epic_"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WNDMainWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WNDMainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        return it;
    }

    private void initBackgroundImage() {
        try {
            Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
            BufferedImage image = ImageIO.read(this.getClass().getResourceAsStream("/br/manaux/ux/gfx/background.png"));
            jLbBackground.setSize(screenDimension);
            jLbBackground.setIcon(new ImageIcon(image.getScaledInstance(screenDimension.width, screenDimension.height, BufferedImage.TYPE_INT_RGB)));

        } catch (IOException ex) {
            Logger.getLogger(WNDMainWindow.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initMoneyLabel() {
        jLbRunningMoney = new JLabel();

        startRunningMoneyPos = new Point(jTpostitMoney.getLocation().x, jTpostitMoney.getLocation().y - 20);

        jLbRunningMoney.setForeground(Color.RED);
        jLbRunningMoney.setHorizontalAlignment(JLabel.CENTER);
        jLbRunningMoney.setHorizontalTextPosition(JLabel.CENTER);
        jLbRunningMoney.setFont(new Font("Serif", Font.BOLD, 20));
        jLbRunningMoney.setSize(jTpostitMoney.getSize());
        jLbRunningMoney.setLocation(startRunningMoneyPos);
        jLbRunningMoney.setOpaque(false);
        jLbRunningMoney.setVisible(false);
        jPpainelPostit.add(jLbRunningMoney);
        jPpainelPostit.setComponentZOrder(jLbRunningMoney, 0);
    }

    private void updateMoney() {
        if (money < 0) {
            jTpostitMoney.setForeground(Color.red);
        }
        this.jTpostitMoney.setText("R$ " + money + ",00");
    }

    public void toggleHand() {

        if (!handMoving) {
            handMoving = true;
            new Thread(toggleHandRunnable).start();
        }

    }

    public void animateLocation(Component jLabel1, Point evt) {
        if (!animating) {

            new Thread(new Runnable() {
                @Override
                public synchronized void run() {
                    int fromX, fromY, actualX, actualY;

                    fromX = evt.x - jLabel1.getWidth() / 2;
                    fromY = evt.y - jLabel1.getHeight() / 2;
                    actualX = jLabel1.getX();
                    actualY = jLabel1.getY();

                    while (fromX != actualX || fromY != actualY) {

                        try {
                            wait(0, 60);

                        } catch (InterruptedException ex) {
                            Logger.getLogger(WNDMainWindow.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }

                        if (actualX > fromX) {
                            jLabel1.setLocation(--actualX, actualY);
                        } else if (actualX < fromX) {
                            jLabel1.setLocation(++actualX, actualY);
                        }

                        if (actualY > fromY) {
                            jLabel1.setLocation(actualX, --actualY);
                        } else if (actualY < fromY) {
                            jLabel1.setLocation(actualX, ++actualY);
                        }
                    }

                    animating = false;
                }
            }).start();

            animating = true;
        }
    }

    public void showPostit(Postit it) {
        currentPostit = it;
        jTpostitConteudo.setText(it.getConteudo());
        jTpostitTitulo.setText(it.getTitulo());
        jTpostitFechar.addMouseListener(gameEvents.getFunctionalCloseAdapter());
        jTpostitFechar.setText("Fechar");
        jTpostitFreelancer.setForeground(Color.DARK_GRAY);
        jTpostitFreelancer.setText("Chamar Freelancer (R$ -200,00/hora)");
        jTpostitConteudo.setFont(new Font("Serif", Font.BOLD, (it.getConteudo().length() <= 400 ? 16 : 12)));
        if (it.isFinished()) {
            jTpostitPronto.setText("Validar");
            jTpostitPronto.setBorder(null);
            jTpostitPronto.addMouseListener(gameEvents.getFunctionalStartDoingAdapter());
            jTpostitFechar.setForeground(Color.RED);
            jTpostitPronto.setForeground(Color.GRAY);
        } else {
            jTpostitPronto.addMouseListener(gameEvents.getFunctionalStartDoingAdapter());
            jTpostitPronto.setText("Fazer");
            jTpostitPronto.setBorder(null);
            jTpostitFechar.setForeground(Color.RED);
            jTpostitPronto.setForeground(Color.GRAY);
        }
        toggleHand();
    }

    public synchronized void finishEpicGame() {
        toDo--;
        playing = false;
        currentPostit.setFinished(true);
        currentPostit.endEpic();

        jTpostitFechar.setText("Terminar");
        jTpostitFechar.setForeground(Color.RED);
        jTpostitFechar.setBorder(null);

        jLbBackground.add(new Postit(this, LocationType.TODO, "", "", 0, 0, 0, true).endEpic().title());
        jLbBackground.add(new Postit(this, LocationType.TODO, "", "", 0, 0, 0, true).endEpic().title());
        jLbBackground.add(new Postit(this, LocationType.TODO, "", "", 0, 0, 0, true).endEpic().title());

        for (MouseListener ls : jTpostitPronto.getMouseListeners()) {
            jTpostitPronto.removeMouseListener(ls);
        }

        for (MouseListener ls : jTpostitFechar.getMouseListeners()) {
            jTpostitFechar.removeMouseListener(ls);
        }

        hidePostit();
        if (toDo == 0) {
            jPpainelMao.setVisible(false);
            this.setAlwaysOnTop(false);
            if (money > 0) {
                JOptionPane.showMessageDialog(null, "Parabéns equipe! \n Vocês encerraram o jogo lucrando \nR$ " + money + ",00", "Parabéns", JOptionPane.WARNING_MESSAGE);
            } else if (money == 0) {
                JOptionPane.showMessageDialog(null, "Bom, vocês terminaram o jogo sem dívidas\n\nParece bom!", "Boa", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Poxa equipe, vocês terminaram o jogo devendo \n R$ " + money + ",00", "Ops", JOptionPane.WARNING_MESSAGE);
            }
            this.setAlwaysOnTop(true);
        }
    }

    public synchronized void finishDoingGame() {
        playing = false;
        this.setAlwaysOnTop(false);
        this.addictionalMoneyPerSecond = 0;
        if (currentPostit.isFinished()) {
            int validate = JOptionPane.showConfirmDialog(null, "A equipe de validação aprova a implementação?", "E então...", JOptionPane.INFORMATION_MESSAGE);

            if (validate == JOptionPane.YES_OPTION) {
                currentPostit.moveToNext();
                currentPostit.groupTitle();
                toDo--;
            } else {
                currentPostit.moveToLast();
                currentPostit.setFinished(false);
                currentPostit.changeTitle();
            }
        } else {
            currentPostit.setFinished(true);
            currentPostit.moveToNext();
            currentPostit.changeTitle();
        }
        this.setAlwaysOnTop(true);
        hidePostit();

        for (MouseListener ls : jTpostitPronto.getMouseListeners()) {
            jTpostitPronto.removeMouseListener(ls);
        }

        for (MouseListener ls : jTpostitFechar.getMouseListeners()) {
            jTpostitFechar.removeMouseListener(ls);
        }

        for (MouseListener ls : jTpostitFreelancer.getMouseListeners()) {
            jTpostitFreelancer.removeMouseListener(ls);
        }

        if (toDo == 0) {
            jPpainelMao.setVisible(false);
            this.setAlwaysOnTop(false);
            if (money > 0) {
                JOptionPane.showMessageDialog(null, "Parabéns equipe! \n Vocês encerraram o jogo lucrando \nR$ " + money + ",00", "Parabéns", JOptionPane.WARNING_MESSAGE);
            } else if (money == 0) {
                JOptionPane.showMessageDialog(null, "Bom, vocês terminaram o jogo sem dívidas\n\nParece bom!", "Boa", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Poxa equipe, vocês terminaram o jogo devendo \n R$ " + money + ",00", "Ops", JOptionPane.WARNING_MESSAGE);
            }
            this.setAlwaysOnTop(true);
        }
    }

    public synchronized void startDoingGame() {
        for (MouseListener ls : jTpostitPronto.getMouseListeners()) {
            jTpostitPronto.removeMouseListener(ls);
        }

        for (MouseListener ls : jTpostitFechar.getMouseListeners()) {
            jTpostitFechar.removeMouseListener(ls);
        }

        for (MouseListener ls : jTpostitFreelancer.getMouseListeners()) {
            jTpostitFreelancer.removeMouseListener(ls);
        }

        jTpostitPronto.setBorder(BorderFactory.createLineBorder(Color.MAGENTA, 3, true));
        jTpostitPronto.setText("Fazendo");
        jTpostitPronto.setForeground(Color.MAGENTA);
        jTpostitPronto.paintImmediately(jTpostitPronto.getVisibleRect());
        jTpostitPronto.setBorder(BorderFactory.createLineBorder(Color.MAGENTA, 3, true));

        jTpostitFechar.setText("Terminar");
        jTpostitPronto.paintImmediately(jTpostitPronto.getVisibleRect());

        if (currentPostit.isIsEpic()) {
            for (MouseListener ls : jTpostitFreelancer.getMouseListeners()) {
                jTpostitFreelancer.removeMouseListener(ls);
            }

            jTpostitFreelancer.setText("Freelancer não disponível");
            jTpostitFechar.addMouseListener(gameEvents.getFunctionalStartEpicAdapter());
        } else {
            jTpostitFreelancer.addMouseListener(gameEvents.getFunctionalStartFreelancerAdapter());
            jTpostitFechar.addMouseListener(gameEvents.getFunctionalFinishDoingAdapter());
        }

        playing = true;
        new Thread(runningOutOfMoney).start();

    }

    public void startFreelancer() {
        this.addictionalMoneyPerSecond = 200;
        this.jTpostitFreelancer.setText("Freelancer trabalhando! (R$ -200,00/hora)");
        this.jTpostitFreelancer.setForeground(Color.red);
        for (MouseListener ls : jTpostitFreelancer.getMouseListeners()) {
            jTpostitFreelancer.removeMouseListener(ls);
        }
    }

    public void hidePostit() {
        currentPostit = null;
        toggleHand();
    }

    public boolean isPostitShowing() {
        return postitShowing;
    }

    public HashMap<LocationType, LocationType> getNextLocationType() {
        return nextLocationType;
    }

    public HashMap<LocationType, boolean[]> getFreeSpaceMap() {
        return freeSpaceMap;
    }

    public HashMap<LocationType, JPanel> getPanelMap() {
        return panelMap;
    }

    public HashMap<LocationType, LocationType> getLastLocationType() {
        return lastLocationType;
    }

    public Postit getCurrentPostit() {
        return currentPostit;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPpainelMao = new javax.swing.JPanel();
        jPpainelPostit = new javax.swing.JPanel();
        jTpostitFechar = new javax.swing.JLabel();
        jTpostitTitulo = new javax.swing.JLabel();
        jTpostitConteudo = new javax.swing.JLabel();
        jTpostitPronto = new javax.swing.JLabel();
        jTpostitMoney = new javax.swing.JLabel();
        jTpostitData = new javax.swing.JLabel();
        jTpostitFreelancer = new javax.swing.JLabel();
        jLbMao = new javax.swing.JLabel();
        jPpdone = new javax.swing.JPanel();
        jPpvalidation = new javax.swing.JPanel();
        jPpTodo = new javax.swing.JPanel();
        jPpdoing = new javax.swing.JPanel();
        jLbBackground = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);
        setPreferredSize(new java.awt.Dimension(600, 600));

        jPanel1.setBackground(new java.awt.Color(249, 72, 72));
        jPanel1.setLayout(null);

        jPpainelMao.setOpaque(false);
        jPpainelMao.setLayout(null);

        jPpainelPostit.setOpaque(false);

        jTpostitFechar.setFont(new java.awt.Font("Noto Sans", 1, 24)); // NOI18N
        jTpostitFechar.setForeground(new java.awt.Color(193, 1, 55));
        jTpostitFechar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jTpostitFechar.setText("Fechar");
        jTpostitFechar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        jTpostitTitulo.setFont(new java.awt.Font("Noto Sans", 3, 24)); // NOI18N
        jTpostitTitulo.setForeground(new java.awt.Color(54, 33, 83));
        jTpostitTitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jTpostitTitulo.setText("Titulo");

        jTpostitConteudo.setFont(new java.awt.Font("Noto Sans", 1, 13)); // NOI18N
        jTpostitConteudo.setForeground(new java.awt.Color(1, 1, 1));
        jTpostitConteudo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jTpostitConteudo.setText("Conteúdo");
        jTpostitConteudo.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        jTpostitPronto.setFont(new java.awt.Font("Noto Sans", 1, 32)); // NOI18N
        jTpostitPronto.setForeground(new java.awt.Color(41, 91, 0));
        jTpostitPronto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jTpostitPronto.setText("Fazer");
        jTpostitPronto.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jTpostitPronto.setPreferredSize(new java.awt.Dimension(220, 43));

        jTpostitMoney.setFont(new java.awt.Font("Noto Sans", 1, 44)); // NOI18N
        jTpostitMoney.setForeground(new java.awt.Color(31, 107, 1));
        jTpostitMoney.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jTpostitMoney.setText("R$ 5000,00");

        jTpostitData.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        jTpostitData.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jTpostitData.setText("00/00/0000");

        jTpostitFreelancer.setFont(new java.awt.Font("Noto Sans", 1, 17)); // NOI18N
        jTpostitFreelancer.setForeground(new java.awt.Color(130, 130, 130));
        jTpostitFreelancer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jTpostitFreelancer.setText("Chamar Freelancer (R$ -200/hora)");
        jTpostitFreelancer.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPpainelPostitLayout = new javax.swing.GroupLayout(jPpainelPostit);
        jPpainelPostit.setLayout(jPpainelPostitLayout);
        jPpainelPostitLayout.setHorizontalGroup(
            jPpainelPostitLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPpainelPostitLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPpainelPostitLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTpostitMoney, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTpostitConteudo, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPpainelPostitLayout.createSequentialGroup()
                        .addComponent(jTpostitTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTpostitData))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPpainelPostitLayout.createSequentialGroup()
                        .addGap(0, 79, Short.MAX_VALUE)
                        .addComponent(jTpostitPronto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTpostitFechar))
                    .addGroup(jPpainelPostitLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jTpostitFreelancer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPpainelPostitLayout.setVerticalGroup(
            jPpainelPostitLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPpainelPostitLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPpainelPostitLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTpostitTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTpostitData, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jTpostitConteudo, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23)
                .addComponent(jTpostitMoney)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTpostitFreelancer, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPpainelPostitLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTpostitFechar, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTpostitPronto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPpainelMao.add(jPpainelPostit);
        jPpainelPostit.setBounds(390, 70, 410, 420);

        jLbMao.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/manaux/ux/gfx/hand.png"))); // NOI18N
        jPpainelMao.add(jLbMao);
        jLbMao.setBounds(0, 0, 1366, 768);

        jPanel1.add(jPpainelMao);
        jPpainelMao.setBounds(-20000, 0, 1480, 960);

        javax.swing.GroupLayout jPpdoneLayout = new javax.swing.GroupLayout(jPpdone);
        jPpdone.setLayout(jPpdoneLayout);
        jPpdoneLayout.setHorizontalGroup(
            jPpdoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 200, Short.MAX_VALUE)
        );
        jPpdoneLayout.setVerticalGroup(
            jPpdoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 350, Short.MAX_VALUE)
        );

        jPanel1.add(jPpdone);
        jPpdone.setBounds(850, 140, 200, 350);

        javax.swing.GroupLayout jPpvalidationLayout = new javax.swing.GroupLayout(jPpvalidation);
        jPpvalidation.setLayout(jPpvalidationLayout);
        jPpvalidationLayout.setHorizontalGroup(
            jPpvalidationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 200, Short.MAX_VALUE)
        );
        jPpvalidationLayout.setVerticalGroup(
            jPpvalidationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 350, Short.MAX_VALUE)
        );

        jPanel1.add(jPpvalidation);
        jPpvalidation.setBounds(570, 140, 200, 350);

        javax.swing.GroupLayout jPpTodoLayout = new javax.swing.GroupLayout(jPpTodo);
        jPpTodo.setLayout(jPpTodoLayout);
        jPpTodoLayout.setHorizontalGroup(
            jPpTodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 200, Short.MAX_VALUE)
        );
        jPpTodoLayout.setVerticalGroup(
            jPpTodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 350, Short.MAX_VALUE)
        );

        jPanel1.add(jPpTodo);
        jPpTodo.setBounds(70, 140, 200, 350);

        javax.swing.GroupLayout jPpdoingLayout = new javax.swing.GroupLayout(jPpdoing);
        jPpdoing.setLayout(jPpdoingLayout);
        jPpdoingLayout.setHorizontalGroup(
            jPpdoingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 200, Short.MAX_VALUE)
        );
        jPpdoingLayout.setVerticalGroup(
            jPpdoingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 350, Short.MAX_VALUE)
        );

        jPanel1.add(jPpdoing);
        jPpdoing.setBounds(310, 140, 200, 350);
        jPanel1.add(jLbBackground);
        jLbBackground.setBounds(0, 0, 0, 0);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 822, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(WNDMainWindow.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        }
        //</editor-fold>

        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new WNDMainWindow(Integer.parseInt(args.length > 0 ? args[0] : "100000")).setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLbBackground;
    private javax.swing.JLabel jLbMao;
    private javax.swing.JPanel jPanel1;
    public javax.swing.JPanel jPpTodo;
    private javax.swing.JPanel jPpainelMao;
    private javax.swing.JPanel jPpainelPostit;
    public javax.swing.JPanel jPpdoing;
    public javax.swing.JPanel jPpdone;
    public javax.swing.JPanel jPpvalidation;
    private javax.swing.JLabel jTpostitConteudo;
    private javax.swing.JLabel jTpostitData;
    private javax.swing.JLabel jTpostitFechar;
    private javax.swing.JLabel jTpostitFreelancer;
    private javax.swing.JLabel jTpostitMoney;
    private javax.swing.JLabel jTpostitPronto;
    private javax.swing.JLabel jTpostitTitulo;
    // End of variables declaration//GEN-END:variables
}
