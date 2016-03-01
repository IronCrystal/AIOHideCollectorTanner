package ironcrystal.hidecollectortanner;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.widgets.WidgetChild;

@ScriptManifest(category = Category.MONEYMAKING, name = "AIO Hide Collector and Tanner", author = "IronCrystal", version = 1.0)
public class HideCollectorTanner extends AbstractScript {

	//Cowhide Areas
	public Area cows1 = new Area(3253, 3255, 3265, 3296, 0);
	public Area cows2 = new Area(3240, 3297, 3265, 3278, 0);
	public Area tanner = new Area(3277, 3189, 3270, 3194, 0);

	//Gate
	public Area gate = new Area(3267, 3229, 3268, 3226, 0);

	public int cost;
	public int hidesPicked;

	//Position <= 3267 is lumbridge, >= 3268 is al kharid

	public Status currentStatus;
	public TanType tanType;

	public boolean killCows;

	JFrame frame;
	ButtonGroup group;

	public enum Status {
		GETCOINSFORGATE,
		GOTOCOWS,
		COLLECTHIDES,
		GOTOTANNER,
		TAN,
		BANK;
	}

	public enum TanType {
		LEATHER,
		HARDLEATHER,
		NONE;
	}

	private void updateStatus() {
		if (tanType != null) {
			switch(tanType) {
			case LEATHER: cost = 47; break;
			case HARDLEATHER: cost = 101; break;
			default: cost = 0;		
			}
			if (tanType != TanType.NONE && (getInventory().count(item -> item != null && item.getID() == 995) < 10)) {
				currentStatus = Status.GETCOINSFORGATE;
				return;
			}
			else if (getInventory().emptySlotCount() > 1 && !cows1.contains(getLocalPlayer()) && !cows2.contains(getLocalPlayer())) {
				currentStatus = Status.GOTOCOWS;
				return;
			}
			else if (getInventory().emptySlotCount() > 1 && (cows1.contains(getLocalPlayer()) || cows2.contains(getLocalPlayer()))) {
				currentStatus = Status.COLLECTHIDES;
				return;
			}
			else if (tanType != TanType.NONE && getInventory().isFull() && !tanner.contains(getLocalPlayer())) {
				currentStatus = Status.GOTOTANNER;
				return;
			}
			else if (tanType != TanType.NONE && getInventory().isFull()
					&& tanner.contains(getLocalPlayer())
					&& getInventory().contains(item -> item != null && item.getID() == 1739)
					&& getInventory().count(item -> item != null && item.getID() == 995) >= (cost - 20)) {
				currentStatus = Status.TAN;
			}else{
				currentStatus = Status.BANK;
			}
		}
	}

	@Override
	public void onStart() {
		log("AIO Hide Collector and Tanner started");
		killCows = false;
		hidesPicked = 0;
		updateStatus();

		frame = new JFrame("AIO Hide Collector and Tanner");
		JPanel panel = new JPanel();
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JRadioButton buttonNONE = new JRadioButton("No tanning");
		buttonNONE.setSelected(true);
		JRadioButton buttonLEATHER = new JRadioButton("Leather tanning");
		JRadioButton buttonHARD = new JRadioButton("Hard leather tanning");
		JButton start = new JButton("Start");
		ButtonGroup group = new ButtonGroup();
		group.add(buttonNONE);
		group.add(buttonLEATHER);
		group.add(buttonHARD);
		group.add(start);
		panel.add(buttonNONE);
		panel.add(buttonLEATHER);
		panel.add(buttonHARD);
		panel.add(start);
		frame.add(panel);
		frame.setSize(150, 150);
		frame.setVisible(true);
		frame.setResizable(false);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);

		start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// this makes sure the button you are pressing is the button variable
				if(e.getSource() == start) {
					if (buttonNONE.isSelected()) {
						tanType = TanType.NONE;
						updateStatus();
						frame.setVisible(false);
					}
					else if (buttonLEATHER.isSelected()) {
						tanType = TanType.LEATHER;
						updateStatus();
						frame.setVisible(false);
					}
					else if (buttonHARD.isSelected()) {
						tanType = TanType.HARDLEATHER;
						updateStatus();
						frame.setVisible(false);
					}
				}
			}
		});
	}

	@Override
	public void onPaint(Graphics graphics) {
		graphics.drawString("Current Status: " + currentStatus.toString(), 1, 10);
	}

	@Override
	public int onLoop() {
		if (tanType != null) {
			switch(currentStatus) {
			case GETCOINSFORGATE: return getCoinsFromBank();
			case GOTOCOWS: return goToCows();
			case COLLECTHIDES: return collectHides();
			case GOTOTANNER: return goToTanner();
			case TAN: return tan();
			case BANK: return bank();
			default: return bank();
			}
		}
		return 500;
	}

	private int getCoinsFromBank() {
		if (getInventory().count(item -> item != null && item.getID() == 995) == cost) {
			updateStatus();
			return 500;
		}
		if (getBank().getClosestBankLocation().getArea(3).contains(getLocalPlayer())) {
			if (getBank().isOpen()) {
				if (getBank().depositAllItems()) {
					if (getBank().withdraw(item -> item != null && item.getID() == 995, cost)) {
						if (getBank().close()) {
							sleepUntil(() -> !getBank().isOpen(), 5000);
						}
					}
				}else{
					log("Player has no more money");
					tanType = TanType.NONE;
					updateStatus();
				}
			}else{
				getBank().open();
				sleepUntil(() -> getBank().isOpen(), 5000);
			}
		}else{
			if (getWalking().walk(getBank().getClosestBankLocation().getArea(3).getRandomTile())) {
				sleep(Calculations.random(500, 1000));
				return 500;
			}
		}
		return 500;
	}

	private int goToCows() {
		if (getLocalPlayer().getX() >= 3268) {
			//Need to go through gate
			if (gate.contains(getLocalPlayer())) {
				GameObject entrance = getGameObjects().closest(gameObject -> gameObject != null && gameObject.getName().equals("Gate"));
				if (entrance != null) {
					if (entrance.interact("Pay-toll(10gp)")) {
						sleepUntil(() -> getLocalPlayer().getX() < 3268, 10000);
						return 500;
					}
				}
			}else{
				if (getWalking().walk(gate.getRandomTile())) {
					sleep(Calculations.random(500, 1000));
					return 500;
				}
			}
		}else{
			if (cows1.contains(getLocalPlayer())) {
				updateStatus();
			}else{
				if (getWalking().walk(cows1.getRandomTile())) {
					sleep(Calculations.random(500, 1000));
					return 500;
				}
			}
		}
		return 500;
	}

	private int collectHides() {
		if (cows1.contains(getLocalPlayer()) || cows2.contains(getLocalPlayer())) {
			int count = getInventory().count(item -> item != null && item.getID() == 1739);
			if (getInventory().isFull()) {
				updateStatus();
				return 500;
			}
			GroundItem hide = getGroundItems().closest(item -> item != null && item.getID() == 1739);
			if (hide != null) {
				if (hide.interact("Take")) {
					sleepUntil(() -> getInventory().count(item -> item != null && item.getID() == 1739) > count || hide == null || !hide.exists(), 10000);
				}
			}
		}else{
			if (getWalking().walk(cows1.getRandomTile())) {
				sleep(Calculations.random(500, 1000));
				return 500;
			}
		}
		return 500;
	}

	private int goToTanner() {
		if (getLocalPlayer().getX() <= 3267) {
			//Need to go through gate
			if (gate.contains(getLocalPlayer())) {
				GameObject entrance = getGameObjects().closest(gameObject -> gameObject != null && gameObject.getName().equals("Gate"));
				if (entrance != null) {
					if (entrance.interact("Pay-toll(10gp)")) {
						sleepUntil(() -> getLocalPlayer().getX() < 3268, 10000);
						return 500;
					}
				}
			}else{
				if (getWalking().walk(gate.getRandomTile())) {
					sleep(Calculations.random(500, 1000));
					return 500;
				}
			}
		}else{
			if (tanner.contains(getLocalPlayer())) {
				updateStatus();
			}else{
				if (getWalking().walk(tanner.getCenter())) {
					sleep(Calculations.random(500, 1000));
					return 500;
				}
			}
		}
		return 500;
	}

	private int tan() {
		if (getInventory().count(item -> item != null && item.getID() == 1739) <= 0 || getInventory().count(item -> item != null && item.getID() == 995) < 3) {
			updateStatus();
		}
		if (tanner.contains(getLocalPlayer())) {
			NPC tanDude = getNpcs().closest(npc -> npc != null && npc.getName().equals("Ellis"));
			if (tanDude != null) {
				WidgetChild widget;
				int cost;
				if (tanType == TanType.LEATHER) {
					widget = getWidgets().getChildWidget(324, 92);
					cost = 1;
				}else{
					widget = getWidgets().getChildWidget(324, 93);
					cost = 3;
				}

				if (widget != null && widget.isVisible()) {
					if (widget.interact("Tan All")) {
						sleepUntil(() -> ((getInventory().count(item -> item != null && item.getID() == 1739) == 0) || (getInventory().count(item -> item != null && item.getID() == 995) < cost)), 5000);
					}
				}
				else if (tanDude.interact("Trade")) {
					sleepUntil(() -> widget != null && widget.isVisible(), 5000);
				}
			}
		}else{
			if (getWalking().walk(tanner.getCenter())) {
				sleep(Calculations.random(500, 1000));
				return 500;
			}
		}
		return 500;
	}

	private int bank() {
		if (getBank().getClosestBankLocation().getArea(3).contains(getLocalPlayer())) {
			if (getBank().isOpen()) {
				if (getBank().depositAllItems()) {
					if (getBank().close()) {
						updateStatus();
						sleepUntil(() -> !getBank().isOpen(), 5000);
					}
				}
			}else{
				getBank().open();
				sleepUntil(() -> getBank().isOpen(), 5000);
			}
		}else{
			if (getWalking().walk(getBank().getClosestBankLocation().getArea(3).getCenter())) {
				sleep(Calculations.random(500, 1000));
				return 500;
			}
		}
		return 500;
	}
}
