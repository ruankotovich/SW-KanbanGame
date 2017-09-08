/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.manaux.model.bean;

import br.manaux.ux.gui.WNDMainWindow;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 *
 * @author dmitry
 */
public class Postit extends JLabel implements MouseListener {

    private boolean isMoving = false;
    private boolean isEpic = false;
    private boolean finished = false;
    private boolean blocked = false;
    private final int moneyPerSecond;
    private int position = -1;
    private String titulo;
    private String conteudo;
    private final WNDMainWindow caller;
    private WNDMainWindow.LocationType locationType;
    private String[] team;

    private static final Font POSTIT_FONT = new Font("Serif", Font.BOLD, 18);
    private static final ImageIcon POSTIT_IMAGE = new ImageIcon(Postit.class.getResource("/br/manaux/ux/gfx/postit1.png"));
    private static final ImageIcon POSTITB_IMAGE = new ImageIcon(Postit.class.getResource("/br/manaux/ux/gfx/postit_blocked.png"));

    private final Runnable updatePositionThread = () -> {
        Point p;
        while (isMoving) {
            p = MouseInfo.getPointerInfo().getLocation();
            this.setLocation(p.x - getWidth() / 2, p.y - getHeight() / 2);
        }
    };

    public Postit(WNDMainWindow calling, WNDMainWindow.LocationType location, String title, String content, int money, Integer doingTeam, Integer validationTeam, boolean epic) {
        this.locationType = location;
        this.caller = calling;
        this.isEpic = epic;
        this.titulo = title;
        this.conteudo = content;
        this.moneyPerSecond = money;
        this.setFont(POSTIT_FONT);
        this.moveInsideType(location);
        this.addMouseListener(this);
        this.setHorizontalTextPosition(CENTER);
        this.setSize(POSTIT_IMAGE.getIconWidth(), POSTIT_IMAGE.getIconHeight());
        this.setIcon(POSTIT_IMAGE);
        this.setVisible(true);

        this.team = new String[2];
        this.team[0] = doingTeam.toString();
        this.team[1] = validationTeam.toString();

        this.setText(this.team[0]);
    }

    public void changeTitle() {
        String aux = this.team[0];
        this.team[0] = this.team[1];
        this.team[1] = aux;

        this.setText(this.team[0]);
    }

    public void groupTitle() {
        this.setText(this.team[0] + "+" + this.team[1]);
    }

    public void moveInsideType(WNDMainWindow.LocationType type) {
        boolean[] freeSpace = caller.getFreeSpaceMap().get(type);

        for (int i = 0; i < 12; i++) {
            if (freeSpace[i] == false) {
                this.locationType = type;

                if (this.position >= 0) {
                    freeSpace[this.position] = false;
                }

                freeSpace[i] = true;
                moveBasedOnFree(this.locationType, i, false);
                break;
            }
        }

    }

    public void moveToNext() {
        boolean[] freeSpace;
        freeSpace = caller.getFreeSpaceMap().get(locationType);
        WNDMainWindow.LocationType nextLocation = caller.getNextLocationType().get(this.locationType);
        boolean[] nextFreeSpace = caller.getFreeSpaceMap().get(nextLocation);

        for (int i = 0; i < 12; i++) {
            if (nextFreeSpace[i] == false) {
                this.locationType = nextLocation;

                if (this.position >= 0) {
                    freeSpace[this.position] = false;
                }

                nextFreeSpace[i] = true;
                moveBasedOnFree(this.locationType, i, true);
                break;
            }
        }

    }

    public void moveToLast() {
        boolean[] freeSpace;
        freeSpace = caller.getFreeSpaceMap().get(locationType);
        WNDMainWindow.LocationType nextLocation = caller.getLastLocationType().get(this.locationType);
        boolean[] nextFreeSpace = caller.getFreeSpaceMap().get(nextLocation);

        for (int i = 0; i < 12; i++) {
            if (nextFreeSpace[i] == false) {
                this.locationType = nextLocation;

                if (this.position >= 0) {
                    freeSpace[this.position] = false;
                }

                nextFreeSpace[i] = true;
                moveBasedOnFree(this.locationType, i, true);
                break;
            }
        }

    }

    private void moveBasedOnFree(WNDMainWindow.LocationType type, int position, boolean animated) {
        int posX = caller.getPanelMap().get(type).getX() + (position % 3) * POSTIT_IMAGE.getIconWidth() + this.getWidth() / 2;
        int posY = caller.getPanelMap().get(type).getY() + (position / 3) * POSTIT_IMAGE.getIconHeight() + this.getHeight() / 2;
        this.position = position;
        if (animated) {
            caller.animateLocation(this, new Point(posX, posY));
        } else {
            this.setLocation(new Point(posX, posY));
        }

    }

    public Postit title() {
        this.setText("Épico");
        this.setFont(new Font("Serif", Font.ITALIC, 12));
        return this;
    }

    public Postit endEpic() {
        this.locationType = WNDMainWindow.LocationType.FINISHED;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public int getMoneyPerSecond() {
        return moneyPerSecond;
    }

    public boolean isIsMoving() {
        return isMoving;
    }

    public void setIsMoving(boolean isMoving) {
        this.isMoving = isMoving;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized void setFinished(boolean finished) {
        this.finished = finished;
    }

    public WNDMainWindow.LocationType getLocationType() {
        return locationType;
    }

    public boolean isIsEpic() {
        return isEpic;
    }

    public void setIsEpic(boolean isEpic) {
        this.isEpic = isEpic;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        if (blocked) {
            this.setIcon(POSTITB_IMAGE);
        } else {
            this.setIcon(POSTIT_IMAGE);
        }
        this.blocked = blocked;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

        switch (this.locationType) {
            case TODO:
                moveToNext();
                break;
            case DOING:
            case VALIDATION:
                if (!caller.isPostitShowing()) {
                    caller.showPostit(this);
                }

                break;
            default:
                break;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        isMoving = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

}
