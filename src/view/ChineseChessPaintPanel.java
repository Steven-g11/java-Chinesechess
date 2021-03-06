package view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.sound.midi.MidiDevice.Info;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
//import javax.xml.bind.Marshaller.Listener;

import game.CallBack;
import game.ChessBoard;
import game.ChessColor;
import game.ChinessChessGame;
import game.item.chess.Chess;
import game.player.Player;
import media.SoundPlayer;

public class ChineseChessPaintPanel extends JPanel implements CallBack, MouseListener, MouseMotionListener, KeyListener{
	private final int CHESS_SIZE = 55;
	private final int SELECTED_BLOCK_SIZE = 58;
	private final int OFFSET_X = 25;
	private final int OFFSET_Y = 24;
	private final int OFFSET_SELECTED_X = 1;
	private final int OFFSET_SELECTED_Y = 4;
	private Image backgroundImage;
	private Image selectedImageRed;
	private Image selectedImageBlack;
	private	ChinessChessGame chessGame;
	private ChessBoard chessBoard;
	
	private Player currentTurnPlayer;
	private Dimension[][] chessDimensionBlock = new Dimension[10][9]; // used to determine all the chesses' location
	private Dimension closestToMouseDimension;  //used to draw the selection block
	private Chess selectedChess;
	
	public ChineseChessPaintPanel(ChinessChessGame chessGame, Image backgroundImage) throws IOException{
		this.chessGame = chessGame;
		this.backgroundImage = backgroundImage;
		this.selectedImageRed = ImageIO.read(new File("selected_red.png"));
		this.selectedImageBlack = ImageIO.read(new File("selected_black.png"));
		setupDimensionBlock();
		setFocusable(true);  //ensure that the keyboard event can be passed into the panel.
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		chessGame.setCallback(this);
		chessGame.startGame();
	}
	
	private void setupDimensionBlock() {
		final int ORG_X = 52;  //original
		final int ORG_Y = 37;
        
        for (int y = 0 ; y < 10 ; y ++)
        	for (int x = 0 ; x < 9 ; x ++)
        		chessDimensionBlock[y][x] = new Dimension(ORG_X + x*59  , ORG_Y + y*61);
	}

	@Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImage, 0, 0, null);
        drawAllChesses(g);
        drawMouseBlock(g);
        drawSelectedChessBlock(g);
    }
	
	private void drawAllChesses(Graphics g){
		Chess[][] chessStatus = chessBoard.getChesses();
        for (int y = 0 ; y < 10 ; y ++)
        	for (int x = 0 ; x < 9 ; x ++)
        	{
        		Dimension dimension = chessDimensionBlock[y][x];
        		if (chessBoard.hasChess(x, y))
	        		g.drawImage(chessStatus[y][x].getImage(), (int)(dimension.getWidth() - OFFSET_X), (int)(dimension.getHeight() - OFFSET_Y), 
	        				CHESS_SIZE, CHESS_SIZE, null);
        	}
	}
	
	private void drawMouseBlock(Graphics g){
		if (closestToMouseDimension != null)
        {
        	g.drawImage(getSelectedImageByCurrectPlayer(), (int)(closestToMouseDimension.getWidth() - OFFSET_SELECTED_X - OFFSET_X), 
        			(int)(closestToMouseDimension.getHeight() - OFFSET_SELECTED_Y - OFFSET_Y), 
        				SELECTED_BLOCK_SIZE, SELECTED_BLOCK_SIZE, null);
        }
	}

	private void drawSelectedChessBlock(Graphics g){
		if (selectedChess != null)
        {		
        	Dimension selectedDimension = chessDimensionBlock[selectedChess.getY()][selectedChess.getX()];
        	g.drawImage(getSelectedImageByCurrectPlayer(), (int)(selectedDimension.getWidth() - OFFSET_SELECTED_X - OFFSET_X), 
        			(int)(selectedDimension.getHeight() - OFFSET_SELECTED_Y - OFFSET_Y), 
        				SELECTED_BLOCK_SIZE, SELECTED_BLOCK_SIZE, null);
        }
	}
	
	private Image getSelectedImageByCurrectPlayer(){
		return currentTurnPlayer.getTeam() == ChessColor.RED ? selectedImageRed : selectedImageBlack;
	}

	@Override
	public void onGameStarted() {
		repaint();  // paint the prepared chesses
	}

	@Override
	public void onPlayerTurn(Player player) {
		currentTurnPlayer = player;
	}

	@Override
	public void onGameStatusUpdated(ChessBoard chessBoard) {
		this.chessBoard = chessBoard;
		repaint();
	}

	@Override
	public void onMoveRejected(Player player, Chess chess) {
		SoundPlayer.getSoundManager().playSound("media/error.wav");
	}
	
	@Override
	public void onMoveSuccessfully(Player player, Chess chess) {
		SoundPlayer.getSoundManager().playSound("media/put.wav");
	}

	@Override
	public void onGameOver(Player winner) {
		SoundPlayer.getSoundManager().playSound("media/gameover.wav");
		System.out.println("The winner is " + winner.getTeam().toString() + ".");
		JOptionPane.showMessageDialog(null, "最终" + winner.getName() +
				 (winner.getTeam() == ChessColor.RED ? "(红方)" : "(黑方)") + " 胜利", 
		"游戏结束", JOptionPane.PLAIN_MESSAGE);
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {
		Dimension dimension = getClosestDimension(e.getX(), e.getY());
		int x = 0, y = 0;
		
		OutLoop:
		for (int i = 0 ; i < 10 ; i ++)
			for (int j = 0 ; j < 9 ; j ++)
				if (dimension == chessDimensionBlock[i][j])
				{
					y = i;
					x = j;
					break OutLoop;
				}

		handleSelectedEvent(x, y);
		repaint();
	}
	
	private void handleSelectedEvent(int x, int y){
		Chess chess = chessBoard.getChess(x, y);
		
		//if this is a selection phase, you cannot choose the opposite color.
		if (selectedChess == null && chess != null && chess.getColor() != currentTurnPlayer.getTeam())
			onMoveRejected(currentTurnPlayer, chess);
		else
		{
			if (selectedChess == null || (chess != null && chess.getColor() == currentTurnPlayer.getTeam())) //finish the selection phase.
				selectedChess = chess;
			else
			{
				chessGame.moveChess(currentTurnPlayer, selectedChess, x, y); //moving phase.
				selectedChess = null;
				closestToMouseDimension = null;
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {
		closestToMouseDimension = null;
		repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {
		closestToMouseDimension = getClosestDimension(e.getX(), e.getY());
		repaint();
	}
	
	private Dimension getClosestDimension(int x, int y){
		double closedOffset = 1000;
		Dimension closestDimension = new Dimension(1000, 1000);
		for (int i = 0 ; i < 10 ; i ++)
        	for (int j = 0 ; j < 9 ; j ++)
        	{
        		Dimension dimension = chessDimensionBlock[i][j];
        		int offsetX = Math.abs(x - (int)dimension.getWidth());
        		int offsetY = Math.abs(y - (int)dimension.getHeight());
        		double offsetZ = Math.sqrt(offsetX*offsetX + offsetY*offsetY);

        		if (offsetZ < closedOffset)
        		{
        			closedOffset = offsetZ;
        			closestDimension = dimension;
        		}
        	}
		
		return closestDimension;
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_R)
		{
			int selectedOption = JOptionPane.showConfirmDialog(null, 
                    "确定要悔棋吗 ?", 
                    "悔棋操作", 
                    JOptionPane.YES_NO_OPTION); 
			if (selectedOption == JOptionPane.YES_OPTION) {
				chessGame.rollback();
			}
		}
			
	}

	
}
