package pet.ui.hp;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.text.DateFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import pet.hp.*;
import pet.hp.state.*;
import pet.ui.PokerFrame;
import pet.ui.eq.CalcPanel;
import pet.ui.eq.PokerItem;
import pet.ui.ta.*;

/**
 * displays a table with details for the last hand
 */
public class LastHandPanel extends JPanel implements HistoryListener {
	
	private final JComboBox stateCombo = new JComboBox(new DefaultComboBoxModel());
	private final MyJTable handTable = new MyJTable();
	private final JButton prevButton = new JButton(PokerFrame.LEFT_TRI);
	private final JButton nextButton = new JButton(PokerFrame.RIGHT_TRI);
	private final JButton equityButton = new JButton("Equity");
	private final JButton playerButton = new JButton("Player");
	private final JButton replayButton = new JButton("Replay");
	private final JToggleButton updateButton = new JToggleButton("Auto Select");
	private final JScrollPane tableScroller = new JScrollPane(handTable);

	public LastHandPanel() {
		super(new BorderLayout());

		stateCombo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				HandStates states = (HandStates) e.getItem();
				tableScroller.setBorder(new TitledBorder(states.hand.game.id));
				((HandStateTableModel)handTable.getModel()).setRows(states.states);
			}
		});

		prevButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectState(-1);
			}
		});

		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectState(1);
			}
		});
		
		playerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// get player name for selected row
				int r = handTable.getSelectionModel().getMinSelectionIndex();
				if (r >= 0) {
					// not really needed, can't be sorted
					int sr = handTable.convertRowIndexToModel(r);
					HandStateTableModel m = (HandStateTableModel) handTable.getModel();
					HandState hs = m.getRow(sr);
					SeatState as = hs.actionSeat();
					if (as != null) {
						String player = as.seat.name;
						PokerFrame.getInstance().displayPlayer(player);
					}
				}
			}
		});

		equityButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				HandStates hs = (HandStates) stateCombo.getSelectedItem();
				String type;
				switch (hs.hand.game.type) {
					case Game.HE_TYPE:
					case Game.OM_TYPE:
					case Game.FCD_TYPE:
					case Game.STUD_TYPE:
						type = PokerItem.HIGH;
						break;
					case Game.OMHL_TYPE:
					case Game.STUDHL_TYPE:
						type = PokerItem.HILO;
						break;
					case Game.DSSD_TYPE:
					case Game.DSTD_TYPE:
						type = PokerItem.DSLOW;
						break;
					default:
						throw new RuntimeException("unknown game type " + hs.hand);
				}
				
				PokerFrame pf = PokerFrame.getInstance();
				CalcPanel calcPanel = pf.displayCalcPanel(hs.hand.game.type);
				// TODO maybe take hands for selected street instead
				List<String[]> cards = HandUtil.getFinalCards(hs.hand);
				calcPanel.displayHand(hs.hand.board, cards, type);
			}
		});

		replayButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				HandStates hs = (HandStates) stateCombo.getSelectedItem();
				if (hs != null) {
					PokerFrame.getInstance().replayHand(hs.hand);
				}
			}
		});
		
		handTable.setModel(new HandStateTableModel());
		handTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		updateButton.setSelected(true);
		
		JPanel topPanel = new JPanel();
		topPanel.add(prevButton);
		topPanel.add(stateCombo);
		topPanel.add(nextButton);
		topPanel.add(updateButton);

		JPanel bottomPanel = new JPanel();
		bottomPanel.add(equityButton);
		bottomPanel.add(playerButton);
		bottomPanel.add(replayButton);
		
		add(topPanel, BorderLayout.NORTH);
		add(tableScroller, BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH);
	}


	private void selectState(int off) {
		int i = stateCombo.getSelectedIndex();
		if (i >= 0) {
			i += off;
			if (i >= 0 && i < stateCombo.getItemCount()) {
				System.out.println("setting index " + i);
				stateCombo.setSelectedIndex(i);
				repaint();
			}
		}
	}
	
	@Override
	public void handAdded(final Hand hand) {
		long t = System.currentTimeMillis() - (1000 * 60 * 10);
		if (hand.date.getTime() > t) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					showHand(hand);
				}
			});
		}
	}

	@Override
	public void gameAdded(Game game) {
		//
	}

	public void showHand(Hand hand) {
		// create hand states, add to list
		// display most recent in hud
		DefaultComboBoxModel model = (DefaultComboBoxModel)stateCombo.getModel();

		for (int n = 0; n < model.getSize(); n++) {
			HandStates hs = (HandStates) model.getElementAt(n);
			if (hs.hand == hand) {
				// already present, just select
				stateCombo.setSelectedIndex(n);
				return;
			}
		}

		if (model.getSize() > 100) {
			model.removeElementAt(0);
		}
		
		// FIXME may add out of order...
		// XXX does equity sample calc on awt thread... 
		model.addElement(new HandStates(hand));
		if (updateButton.isSelected()) {
			stateCombo.setSelectedIndex(stateCombo.getModel().getSize() - 1);
		}
	}



}

/** represents a list of hand states for a hand */
class HandStates {
	public final List<HandState> states;
	public final Hand hand;
	public HandStates(Hand hand) {
		this.hand = hand;
		this.states = HandStateUtil.getStates(hand);
	}
	@Override
	public String toString() {
		// user readable description of hand
		return hand.tablename + " " + DateFormat.getDateTimeInstance().format(hand.date) + (hand.showdown ? " *" : "");
	}
}
