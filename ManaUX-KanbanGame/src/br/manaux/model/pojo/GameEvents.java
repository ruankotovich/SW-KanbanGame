/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.manaux.model.pojo;

import br.manaux.ux.gui.WNDMainWindow;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 *
 * @author dmitry
 */
public class GameEvents {

    private final WNDMainWindow caller;
    private final MouseAdapter functionalCloseAdapter;
    private final MouseAdapter functionalStartDoingAdapter;
    private final MouseAdapter functionalStartEpicAdapter;
    private final MouseAdapter functionalFinishDoingAdapter;
    private final MouseAdapter functionalStartFreelancerAdapter;

    public GameEvents(WNDMainWindow caller) {

        this.functionalStartEpicAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                caller.getCurrentPostit().setBlocked(true);
                caller.finishEpicGame();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel p = ((JLabel) e.getSource());
                p.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true));
                p.setText("Épico");
                p.setForeground(Color.BLACK);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel p = ((JLabel) e.getSource());
                p.setBorder(null);
                p.setText("Terminar");
                p.setForeground(Color.RED);
            }
        };

        this.functionalStartDoingAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                caller.startDoingGame();
            }
        };

        this.functionalCloseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                caller.hidePostit();
            }

        };

        this.functionalFinishDoingAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                caller.finishDoingGame();
            }

        };

        this.functionalStartFreelancerAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                caller.startFreelancer();
            }

        };

        this.caller = caller;
    }

    public MouseAdapter getFunctionalStartDoingAdapter() {
        return functionalStartDoingAdapter;
    }

    public MouseAdapter getFunctionalCloseAdapter() {
        return functionalCloseAdapter;
    }

    public MouseAdapter getFunctionalFinishDoingAdapter() {
        return functionalFinishDoingAdapter;
    }

    public MouseAdapter getFunctionalStartEpicAdapter() {
        return functionalStartEpicAdapter;
    }

    public MouseAdapter getFunctionalStartFreelancerAdapter() {
        return functionalStartFreelancerAdapter;
    }

}
