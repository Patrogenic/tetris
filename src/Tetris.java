/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
/**
 *
 * @author pelzinga010800
 */
class Window extends JFrame{

    public Window(){
        initWindow();
    }
    private void initWindow(){
        add(new UserInterface());
        
        setVisible(true);
        setSize(600, 500);
        
        setTitle("Tetris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
}
class UserInterface extends JPanel{
    Board board;
    int f = 20; //Temporary
    //Make constants for sizes and easy scaling later
    
    public UserInterface(){
        initUserInterface();
    }
    
    private void initUserInterface(){
        setFocusable(true); //Consider the importance of this
        setBackground(Color.BLACK);
        setSize(200,200);
        
        board = new Board();
    }
    
    @Override
    public void paintComponent(Graphics g){ //Consider paincomponent
        //http://stackoverflow.com/questions/18816251/calling-the-paint-method-from-another-class
        super.paintComponent(g);
        drawBackground(g);
        board.drawBoard(g);
        board.drawGhostTetromino(g);
        board.drawTetromino(g); //Redraw tetromino
        board.drawText(g);
        board.drawHoldPiece(g);
        if(!board.gameRunning){
            board.drawGameOverScreen(g);
        }
    }
    
    private void drawBackground(Graphics g){
        //g.setColor(Color.RED);
        //g.fillRect(5, 5, 15, 15);
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect((int)(f * 6.5), (int)(f*1.5), f*11, f*21); //Make everything whole numbers, not sure how realistic that is
        g.setColor(Color.GRAY);
        g.fillRect(f*7, (int)(f * 2), f*10, f*20);
    }
    class Board implements ActionListener{
        Timer timer;
        Square board[][];
        int SIDE = f; //Length and width of the square units
        int initY = 40; //Cordinate of the moving peice, might need to be different depending on the piece
        int initX = 200; //Cordinate of the moving peice
        int rotationState = 1;
        int score = 0;
        int level = 1; //Difficulty- speed of falling tetromino -also multiplier for lines cleared scoring
        int levelSpeed = 10; //10 is the slowest, 1 is the fastest
        int linesCleared = 0;
        int linesToLevel = 5; //Lines required to level up
        int tickRate = 1000; //In milliseconds
        boolean swapped = false;
        boolean pauseState = false;
        
        Random rand = new Random();
        float r;
        float gr;
        float b;
        Color randomColor;
        Color currentColor;
        Color holdPieceColor;
        int rndShape;
        int holdPiece;
        CurrentCordinates currentCords[]; //Cordinates of the current peice
        CurrentCordinates ghostCords[]; //Cordinates of the ghost peice
        CurrentCordinates holdCords[]; //Cordinates of the piece in holding
        boolean gameRunning = true;
        
        public Board(){
            board = new Square[10][20];
            currentCords = new CurrentCordinates[4];
            ghostCords = new CurrentCordinates[4];
            addKeyListener(new TAdapter());
            initBoard();
            
            
            holdCords = new CurrentCordinates[4];
            for (int i = 0; i < 4; i++) {
                holdCords[i] = new CurrentCordinates(0, 0);
            }
            
            FallingShape fallingShape = new FallingShape();
            timer = new Timer();
            timer.schedule(fallingShape, 0, tickRate); //Updates the position of the falling shape
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            
        }
        
        //Class to store cordinates of each square
        class CurrentCordinates{
            int x;
            int y;
            public CurrentCordinates(int x, int y){
                this.x = x;
                this.y = y;
            }
        }
        
        //Class to update the falling shape
        class FallingShape extends TimerTask{
            FallingShape(){
                spawnTetromino();
                checkSpawnCollision();
                updateGhostTetromino();
            }
            @Override
            public void run(){ //Check collision in this function, also check collision when the player moves the peice
                fallingShape();
            }       
        }
        //Function moves the piece downward every game tick
        public void fallingShape(){
            if(gameRunning){
                //Checks to see if the piece has landed or not
                if(checkCollisionDown()){ //If collided, then the tetris peice stops moving and the squares are put in an array
                    int i, j;
                    
                    //Based on the current cordinates, the position in the array is determined
                    //The color of that position will then be updated
                    for (int k = 0; k < 4; k++) {
                        i = (currentCords[k].x/SIDE) - 7; //7 is the amount of space relative to the edge of the screen
                        j = -(currentCords[k].y/SIDE) + 20 + 1; //20 because of the amount of spaces to the bottom, 1 to compensate for the space above the playing field
                        if(j <= 19){
                            board[i][j].color = currentColor;
                        }else{
                            gameRunning = false; //Piece has landed above the board, game over
                        }
                    }

                    initY = 40; //Resets the initY
                    rotationState = 1; //Resets rotation state to 1 for the new shape
                    swapped = false;
                    
                    //Checks to see if the game is over or not
                    //Checks for full lines and initializes a new shape
                    if(gameRunning){
                        clearLine();
                        spawnTetromino();//Create new random shape to draw with a new number
                        checkSpawnCollision();
                        updateGhostTetromino();
                        repaint();
                    }
                }else{
                    //On each game tick the piece moves down one unit
                    for (int k = 0; k < 4; k++) {
                        currentCords[k].y += 20;
                    }
                    repaint();
                }
            }
        }
        //Function checks to see if there are any full lines to clear and count
        public void clearLine(){
            boolean lineFound;
            int countLines;
            int tempLinesCleared = 0;

            for (int j = 0; j < 20; j++) {
                lineFound = true; //A found line will be true by defualt
                for (int i = 0; i < 10; i++) {
                    if(board[i][j].color == Color.GRAY){
                        lineFound = false; //If there is a space in the row, there is no line to clear
                        break;
                    }
                }
                if(lineFound){ //Determine how many lines were cleared
                    for (int i = 0; i < 10; i++) {
                        for (int k = j; k < 19; k++) {
                            board[i][k].color = board[i][k+1].color;
                        }
                    }
                    //score += 100 * level; //If the number of lines cleared was 1
                    tempLinesCleared++;
                    j--;
                }
                countLines = 0; //Finds an empty line, if there is an empty line the loop stops moving the peices down
                if(j < 19){
                    for (int i = 0; i < 10; i++) {
                        if(board[i][j + 1].color == Color.GRAY){
                            countLines++;
                        }
                    }
                }
                if(countLines == 10){
                    break;
                }
            }
            //Here, check the number of lines cleared
            System.out.println(tempLinesCleared);
            linesCleared += tempLinesCleared;
            linesToLevel -= tempLinesCleared;

            if(tempLinesCleared == 1){ //We'll be playing sounds in each of these conditions in the future
                score += 100 * level;
                playSound("SFX_SpecialLineClearSingle.wav");
            }else if(tempLinesCleared == 2){
                score += 300 * level;
                playSound("SFX_SpecialLineClearDouble.wav");
            }else if(tempLinesCleared == 3){
                score += 500 * level;
                playSound("SFX_SpecialLineClearTriple.wav");
            }else if(tempLinesCleared == 4){
                score += 800 * level;
                playSound("SFX_SpecialTetris.wav");
            }
            if(linesToLevel <= 0){ //Level up + sound effect
                level++;
                linesToLevel = (level * 5) + linesToLevel;

                levelSpeed--;
                tickRate = ((levelSpeed-1)* (10 * levelSpeed)) + 100;
                timer.cancel();
                timer = new Timer();
                timer.schedule(new FallingShape(), 0, tickRate); //Dont need timerTask

                playSound("SFX_LevelUp.wav");
            }
        }
        private boolean checkCollisionDownSub(CurrentCordinates lowestCords[], int i, int j){
            boolean collision = false;
            
            for (int l = 0; l < 4; l++) {
                if(lowestCords[l] != null && lowestCords[l].y != 0){ //currentCords[l].y == lowestY 
                    j = (lowestCords[l].y/20) - 1;
                    i = (lowestCords[l].x/20) - 7;
                    if(j >= 20){ //Tetromino has reached the bottom of the game
                        collision = true;
                        break;
                    }else if(board[i][19-j].color != Color.GRAY){ //Tetromino has has reached other squares below itself
                        collision = true;
                        break;
                    }else{ //Tetromino has no surrounding squares and can move 
                        collision = false;
                    }
                }
            }
            return collision;
        }
        private CurrentCordinates[] findLowestPieces(CurrentCordinates lowestCords[]){ //Lowest pieces in shape
            int j;
            int lowestY = currentCords[0].y;
            
            for (int i = 0; i < 4; i++) {
                lowestCords[i] = new CurrentCordinates(0,0);
            }
            
            //Finds the lowest cordinates in the shape to check only below the bottom squares //Doesn't check below bottom squares actually
            for (int k = 0; k < 4; k++) { //Not sure if I need this or not
                j = currentCords[k].y;
                if(lowestY < j){
                    lowestY = j;
                }
            }
            //BUG: S, Z, T shape don't work for this //Might, should be fixed
            for (int k = 0; k < 3; k++) {
                if(currentCords[k].y - 20 == currentCords[k+1].y && currentCords[k].x == currentCords[k + 1].x){
                    lowestCords[k].y = currentCords[k].y;
                    lowestCords[k].x = currentCords[k].x;
                    k++;
                }else{
                    lowestCords[k].y = currentCords[k].y;
                    lowestCords[k].x = currentCords[k].x;
                }
            }
            if(!(currentCords[3].y + 20 == currentCords[2].y)){
                lowestCords[3].y = currentCords[3].y;
                lowestCords[3].x = currentCords[3].x;
            }
            
            return lowestCords;
        }
        public boolean checkCollisionDown(){ //Checking y collision currently
            int j = 0;
            int i = 0;
            int lowestY = currentCords[0].y;
            CurrentCordinates lowestCords[] = new CurrentCordinates[4];
            lowestCords = findLowestPieces(lowestCords); //Nifty

            return checkCollisionDownSub(lowestCords, i, j); //return new function
        }
        public boolean checkCollisionLeft(){
            boolean collision = false;
            int j;
            int i;
            int leftestX = currentCords[0].x;

            for (int k = 0; k < 4; k++) {
                j = currentCords[k].x;
                if(leftestX > j){ //doublecheck this
                        leftestX = j;
                }
            }
            for (int l = 0; l < 4; l++) {
                if(currentCords[l].x == leftestX && currentCords[l].y > 40){
                    j = (currentCords[l].y/20) - 2;
                    i = (currentCords[l].x/20) - 7;
                    if(i == 0){ //Tetromino has reached the left edge of the game //This might not be neccesary
                        collision = true;
                        break;
                    }else if(board[i - 1][19 - j].color != Color.GRAY){ //Tetromino has has reached other squares below itself
                        collision = true;
                        break;
                    }else{ //Tetromino has no surrounded squares and can move 
                        collision = false;
                    }
                }
            }
            return collision;
        }
        public boolean checkCollisionRight(){
	boolean collision = false;
	int j;
	int i;
	int rightestX = currentCords[3].x; //rightestX = 3

	for (int k = 0; k < 4; k++) {
		j = currentCords[k].x;
		if(rightestX < j){ //doublecheck this
                    rightestX = j;
		}
	}
	for (int l = 0; l < 4; l++) {
            if(currentCords[l].x == rightestX && currentCords[l].y > 40){
                    j = (currentCords[l].y/20) - 2;
                    i = (currentCords[l].x/20) - 7;
                    if(i == 9){ //Tetromino has reached the right edge of the game //This might not be neccesary
                        collision = true;
                        break;
                    }else if(board[i + 1][19 - j].color != Color.GRAY){ //Tetromino has has reached other squares below itself
                        collision = true;
                        break;
                    }else{ //Tetromino has no surrounded squares and can move 
                        collision = false;
                    }
                }
            }
            return collision;
        }
        private void updateGhostTetromino(){
            int i = 0, j = 0;
            CurrentCordinates lowestCords[] = new CurrentCordinates[4];
            lowestCords = findLowestPieces(lowestCords); //Nifty
            CurrentCordinates tempCords[] = new CurrentCordinates[4];
            
            for (int k = 0; k < 4; k++) {
                tempCords[k] = new CurrentCordinates(currentCords[k].x, currentCords[k].y);
            }
            
            //tempCords = currentCords; //Potential unexpected behavior

            while(!checkCollisionDownSub(lowestCords, i, j)){
                for (int k = 0; k < 4; k++) {
                        if(lowestCords[k].y != 0){
                                lowestCords[k].y += 20;
                        }
                        tempCords[k].y += 20;
                }
            }
            ghostCords = tempCords;
            repaint();
        }
        private void initBoard(){
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 20; j++) {//g.setColor(Color.GRAY); //Method applied to show the board filled with random colors
                    board[i][j] = new Square(Color.GRAY);
                }
            }
        }
        public void drawBoard(Graphics g){
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 20; j++) {
                    //g.setColor(setRndColors(j,j)); //Method applied to show the board filled with random colors
                    //if(!(board[j][j].color == Color.GRAY)){ //Not equal to grey instead
                    if(board[i][j].color == Color.GRAY){
                        g.setColor(Color.DARK_GRAY);
                    }else{
                        g.setColor(Color.BLACK);
                    }
                    g.fillRect((i*SIDE) + (SIDE * 7), (int)((-j*SIDE) + (SIDE * 21)), 20, 20);
                    
                    g.setColor(board[i][j].color); //Method for the actual game gets the color of the square
                    g.fillRect((i*SIDE) + (SIDE * 7) + 1, (int)((-j*SIDE) + (SIDE * 21)) + 1, SIDE - 2, SIDE - 2);
                    //}
                }
            }
        }
        //For testing purposes to see the board filled with random colors
        //Testing to see the board displayed properly
        private Color setRndColors(int i, int j){
            r = rand.nextFloat();
            gr = rand.nextFloat();
            b = rand.nextFloat();
            board[i][j] = new Square(new Color(r, gr, b));
            return board[i][j].color;
        }
        //One function takes care of assigning all the Cordinates of the current tetromino
        private CurrentCordinates[] assignCords(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, CurrentCordinates asignCords[]){
            asignCords[0] = new CurrentCordinates(x1, y1);
            asignCords[1] = new CurrentCordinates(x2, y2); 
            asignCords[2] = new CurrentCordinates(x3, y3); 
            asignCords[3] = new CurrentCordinates(x4, y4); 
            //currentCords[0] = new CurrentCordinates(x1, y1);
            //currentCords[1] = new CurrentCordinates(x2, y2); 
            //currentCords[2] = new CurrentCordinates(x3, y3); 
            //currentCords[3] = new CurrentCordinates(x4, y4); 
            //checkSpawnCollision();
            return asignCords;
        }
        private void checkSpawnCollision(){
            int i = 0, j = 0;
            for (int k = 0; k < 4; k++) {
                j = (currentCords[k].y/20) - 1;
                i = (currentCords[k].x/20) - 7;
                
                if(board[i][19-j].color != Color.GRAY){
                    System.out.println("Game Over");
                    gameRunning = false;
                    break;
                }
            }
        }
        public void drawGameOverScreen(Graphics g){
            g.setColor(Color.CYAN);
            g.fillRect(115, 150, 250, 140); //g.fillRect(175, 150, 250, 120);
            g.setColor(Color.BLACK);
            g.drawString("Game Over", 190, 220);
        }
        
        //A new random number will be generated to determine the next shape to draw
        public void spawnTetromino(){
            rndShape = (int)(rand.nextFloat() * 7);
            //rndShape = 5;
            buildTetromino(rndShape, initX, initY);
        }
        private void buildTetromino(int rndShape, int x, int y){
            switch (rndShape) {
                case 0: //I shape
                    y -= 20;
                    currentCords = assignCords(200, y, 220, y, 240, y, 260, y, currentCords);
                    currentColor = Color.CYAN;
                    break;
                case 1: //O Shape
                    currentCords = assignCords(220, y, 220, y-20, 240, y, 240, y-20, currentCords);
                    currentColor = Color.YELLOW;
                    break;
                case 2: //T Shape
                    currentCords = assignCords(200, y, 220, y, 220, y-20, 240, y, currentCords);
                    currentColor = Color.MAGENTA;
                    break;
                case 3: //J Shape
                    currentCords = assignCords(200, y, 200, y-20, 220, y, 240, y, currentCords);
                    currentColor = Color.BLUE;
                    break;
                case 4: //L Shape
                    currentCords = assignCords(200, y, 220, y, 240, y, 240, y-20, currentCords);
                    currentColor = Color.ORANGE;
                    break;
                case 5: //S Shape
                    currentCords = assignCords(200, y, 220, y, 220, y-20, 240, y-20, currentCords);
                    currentColor = Color.GREEN;
                    break;
                case 6: //Z Shape
                    currentCords = assignCords(200, y-20, 220, y, 220, y-20, 240, y, currentCords);
                    currentColor = Color.RED;
                    break;
                default:
                    break;
            }
        }
        
        //Draws the tetromino as it moves across the board, each shape is specified
        //I will have to define an x Cordinate for when the player is able to move the shape
        public void drawTetromino(Graphics g){
            //g.setColor(currentColor);
            
            //Elegant solution to drawing after I have defined Cordinates
            //Though I still need a better way of defining the Cordinates initally
            
            for (int i = 0; i < 4; i++) {
                //g.setColor(new Color(j*50, j*50, j*50));
                g.setColor(Color.BLACK);
                g.fillRect(currentCords[i].x, currentCords[i].y, 20, 20);
                
                g.setColor(currentColor);
                g.fillRect(currentCords[i].x + 1, currentCords[i].y + 1, 18, 18);
                //The second is the new solution
            }
        }
        public void drawGhostTetromino(Graphics g){
            //g.setColor(Color.WHITE);
            
            //Elegant solution to drawing after I have defined Cordinates
            //Though I still need a better way of defining the Cordinates initally
            
            for (int i = 0; i < 4; i++) {
                g.setColor(Color.WHITE);
                g.fillRect(ghostCords[i].x, ghostCords[i].y, 20, 20);
                
                
                g.setColor(Color.GRAY);
                g.fillRect(ghostCords[i].x + 1, ghostCords[i].y + 1, 18, 18);
                //The second is the new solution
            }
        }
        public void drawText(Graphics g){
            g.setColor(Color.RED);
            g.setFont(new Font("TimesRoman", Font.PLAIN, 20));
            g.drawString("Score: " + score, 360, 100);
            g.drawString("Lines Cleared: " + linesCleared, 360, 140);
            g.drawString("Level: " + level, 360, 180);
            g.drawString("Lines to Level Up: " + linesToLevel, 360, 220);
        }
        public void drawHoldPiece(Graphics g){
            
            
            for (int i = 0; i < 4; i++) {
                //Creates border
                g.setColor(Color.BLACK);
                g.fillRect(holdCords[i].x, holdCords[i].y, 20, 20);
                
                //Color of square
                g.setColor(holdPieceColor);
                g.fillRect(holdCords[i].x + 1, holdCords[i].y + 1, 18, 18);
            }
        }
        class Square{//Maybe the class is shape
            //A square class within a shape class?
            Color color;
            public Square(Color color){
                this.color = color;
            }
        }
        public void rotationCollision(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4){
            boolean collision = false;
            CurrentCordinates checkCords[] = new CurrentCordinates[4];
            int i;
            int j;
            checkCords[0] = new CurrentCordinates(x1, y1);
            checkCords[1] = new CurrentCordinates(x2, y2);
            checkCords[2] = new CurrentCordinates(x3, y3);
            checkCords[3] = new CurrentCordinates(x4, y4);
            
            for (int k = 0; k < 4; k++) {
                i = (checkCords[k].x/20) - 7;
                j = (checkCords[k].y/20) - 1;
                if(j > 0){
                    if(i > 9 || i < 0 || j < 0 || j > 20){ //j > 20 || j < 1
                        collision = true;
                        break;
                    }else 
                    if(board[i][20-j].color != Color.GRAY){//Don't check the cordinates that don't move
                        collision = true;
                    break;
                    }
                }
            }
            if(!collision){ //If no collision, assign new cordinates
                currentCords = assignCords(x1, y1, x2, y2, x3, y3, x4, y4, currentCords);
                if(rotationState != 4){
                    rotationState++;
                }else{
                    rotationState = 1;
                }
            }
        }
        public void rotate(){
            int x = currentCords[0].x;
            int y = currentCords[0].y;
            int x2 = currentCords[1].x;
            int y2 = currentCords[1].y;
            switch (rndShape) {
                case 0: //I shape
                    if((x2 == x + 20) && (y == y2)){
                        x += 20;
                        y += 20;
                        rotationCollision(x, y+20, x, y, x, y-20, x, y-40);
                        //rotationCollision(x, y, x, y+20, x, y+40, x, y+60);
                    }else{
                        y -= 40;
                        rotationCollision(x-20, y, x, y, x+20, y, x+40, y);
                    }
                    break;
                case 1: //O Shape
                    //O Shape has no rotation, this basically a place holder for now
                    break;
                case 2: //T Shape
                    if(rotationState == 1){
                        y -= 20;
                        rotationCollision(x+20, y+40, x+20, y+20, x+20, y, x+40, y+20);
                    }else if(rotationState == 2){
                        x -= 20;
                        y -= 20;
                        rotationCollision(x, y, x+20, y+20, x+20, y, x+40, y);
                    }else if(rotationState == 3){
                        rotationCollision(x, y, x+20, y+20, x+20, y, x+20, y-20);
                    }else if(rotationState == 4){
                        rotationCollision(x, y, x+20, y, x+20, y-20, x+40, y);
                    }
                    break;
                case 3: //J Shape
                    if(rotationState == 1){
                        x += 20;
                        y += 20;
                        rotationCollision(x, y, x, y-20, x, y-40, x+20, y-40);
                    }else if(rotationState == 2){
                        rotationCollision(x-20, y-20, x, y-20, x+20, y, x+20, y-20);
                    }else if(rotationState == 3){
                        x += 20;
                        y += 20;
                        rotationCollision(x-20, y, x, y, x, y-20, x, y-40);
                    }else if(rotationState == 4){
                        y -= 20;
                        rotationCollision(x, y, x, y-20, x+20, y, x+40, y);
                    }
                    break;
                case 4: //L Shape
                    if(rotationState == 1){
                        rotationCollision(x+20, y+20, x+20, y, x+20, y-20, x+40, y+20);
                    }else if(rotationState == 2){
                        x -= 40;
                        y -= 20;
                        rotationCollision(x+20, y+20, x+20, y, x+40, y, x+60, y);
                    }else if(rotationState == 3){
                        x += 20;
                        y -= 20;
                        rotationCollision(x, y+20, x, y, x, y-20, x-20, y-20);
                    }else if(rotationState == 4){
                        x -= 20;
                        y -= 20;
                        rotationCollision(x, y, x+20, y, x+40, y, x+40, y-20);
                    }
                    break;
                case 5: //S Shape
                    if((x2 == x + 20) && (y2 == y)){ //If S shape is equal to initial state, hasn't been rotated before
                        y -= 20; //The plus and minus 20 might make collision easier to check, the bottom level stays the same
                            //Might not have to check collision on this rotation except for the edge of the board
                        rotationCollision(x, y, x, y-20, x+20, y+20, x+20, y); //The y -= 20 might be significant
                    }else{ //Else the S shape is equal to the rotated state
                        y += 20;
                        rotationCollision(x, y, x+20, y, x+20, y-20, x+40, y-20);
                    }
                    break;
                case 6: //Z Shape
                    if((x2 == x + 20) && (y == y2-20)){
                        y += 40;
                        rotationCollision(x, y-20, x, y-40, x+20, y-40, x+20, y-60);
                    }else{
                        rotationCollision(x, y-20, x+20, y, x+20, y-20, x+40, y);
                    }
                    break;
                default:
                    break;
            }
        }
        private void openSound(String soundFile) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
            File f = new File(soundFile);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(f.toURI().toURL());  
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
            clip.addLineListener(new LineListener() {
            @Override
            public void update(LineEvent myLineEvent) {
            if (myLineEvent.getType() == LineEvent.Type.STOP)
                clip.close();
                }
            });
            
//            Runnable r = new Runnable() {
//                public void run() {
//                    final JToggleButton startStop = new JToggleButton("Stop");
//                    startStop.addActionListener( new ActionListener() {
//                        public void actionPerformed(ActionEvent ae) {
//                            if (startStop.isSelected()) {
//                                clip.stop();
//                                startStop.setText("Start");
//                            } else {
//                                clip.loop(-1);
//                                clip.start();
//                                startStop.setText("Stop");
//                            }
//                        }
//                    } );
//                    clip.loop(-1);
//                    JOptionPane.showMessageDialog(null, startStop);
//                }
//            };
//            SwingUtilities.invokeLater(r);
        }
//            if (!clip.isRunning()){
//                clip.close();
//            }
            //http://stackoverflow.com/questions/2792977/do-i-need-to-close-an-audio-clip
        private void playSound(String soundFile){
            try {
                openSound(soundFile);
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
                Logger.getLogger(UserInterface.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        
        private class TAdapter extends KeyAdapter {

            public TAdapter() {
            }
            @Override
            public void keyReleased(KeyEvent e){

            }
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                    
                if(gameRunning){
                    if(key == KeyEvent.VK_LEFT){
                        if(!checkCollisionLeft()){
                            if(currentCords[0].x > 140){
                                for (int i = 0; i < 4; i++) {
                                    currentCords[i].x -= 20;
                                    repaint();
                                }
                                updateGhostTetromino();
                                playSound("SFX_PieceMoveLR.wav");
                            }
                        //140 to 340, 320 because a shape is 20
                        }
                    }
                    if (key == KeyEvent.VK_RIGHT) {
                        if(!checkCollisionRight()){
                            if(currentCords[3].x < 320){
                                for (int i = 0; i < 4; i++) {
                                    currentCords[i].x += 20;
                                    repaint();
                                }
                                updateGhostTetromino();
                                playSound("SFX_PieceMoveLR.wav");
                            }
                        }
                    }
                    if (key == KeyEvent.VK_UP) {
                        //I have to check collision on each action
                        //Rotation
                        rotate();
                        updateGhostTetromino();
                        repaint();
                        playSound("SFX_PieceRotateLR.wav");
                    }
                    if (key == KeyEvent.VK_DOWN) {
                        if(!checkCollisionDown()){
                            //score++;
                            if(currentCords[0].y < 420){
                                for (int i = 0; i < 4; i++) {
                                    currentCords[i].y += 20;
                                    repaint();
                                }
                                score++; //Score will go up by one for each unit
                            }
                        }
                    }
                    if(key == KeyEvent.VK_SPACE){
                        int i = 0, j = 0;
                        int tempScore = 0;
                        CurrentCordinates lowestCords[] = new CurrentCordinates[4];
                        lowestCords = findLowestPieces(lowestCords); //Nifty

                        while(!checkCollisionDownSub(lowestCords, i, j)){
                            for (int k = 0; k < 4; k++) {
                                if(lowestCords[k].y != 0){
                                    lowestCords[k].y += 20;
                                }
                                currentCords[k].y += 20;
                            }
                            tempScore++;
                        }
                        score += tempScore * 2; //Multiplier of x2 for each unit the tetromino moves with spacebar
                        repaint();
                        playSound("SFX_PieceHardDrop.wav");

                        fallingShape();
                            if(gameRunning){
                            timer.cancel();
                            timer = new Timer();
                            timer.schedule(new FallingShape(), 0, tickRate);
                        }
                    }
                    if(key == KeyEvent.VK_C && !swapped){
                        int x = 60,y = 60;
                        CurrentCordinates swapCords[] = new CurrentCordinates[4];
                        Color swapColor;
                        
                        
                        //Initialize the array of cordinates, it must be done individually
                        for (int i = 0; i < 4; i++) {
                            swapCords[i] = new CurrentCordinates(0,0);
                        }
                        
                        while(rotationState != 1){
                            rotate();
                        }
                        
                        if(holdCords[0].x != 0){
                            for (int i = 0; i < 4; i++) {
                                //swapCords[i].x = holdCords[i].x;
                                //swapCords[i].y = holdCords[i].y;

                                holdCords[i].x = currentCords[i].x;
                                holdCords[i].y = currentCords[i].y;

                                //currentCords[i].x = swapCords[i].x;
                                //currentCords[i].y = swapCords[i].y;
                            }
                            swapColor = holdPieceColor;
                            holdPieceColor = currentColor;
                            currentColor = swapColor;
                        }else{
                            for (int i = 0; i < 4; i++) {
                                holdCords[i].x = currentCords[i].x;
                                holdCords[i].y = currentCords[i].y;
                            }
                            holdPieceColor = currentColor;
                            spawnTetromino();
                            //rndShape = 3;
                            //buildTetromino(rndShape, initX, initY);
                        }
                        
                        
                        
                        while(holdCords[0].x > 40){
                            for (int i = 0; i < 4; i++) {
                                holdCords[i].x -= 20;
                            }

                        }
                        if(holdCords[0].y < 100){
                            while(holdCords[0].y < 100){
                                for (int i = 0; i < 4; i++) {
                                    holdCords[i].y += 20;
                                }
                            }
                        }else if(holdCords[0].y > 100){
                            while(holdCords[0].y > 100){
                                for (int i = 0; i < 4; i++) {
                                    holdCords[i].y -= 20;
                                }
                            }
                        }
                        
//                        while(currentCords[0].x < 200){
//                            for (int i = 0; i < 4; i++) {
//                                currentCords[i].x += 20;
//                            }
//                        }
//                        while(currentCords[0].y > 20){
//                            for (int i = 0; i < 4; i++) {
//                                currentCords[i].y -= 20;
//                            }
//                        }
                        
                        buildTetromino(holdPiece, initX, initY);
                        int swapPiece;
                        swapPiece = holdPiece;
                        
                        
                        holdPiece = rndShape;
                        rndShape = swapPiece;
                        
                        swapped = true;
                        
                        updateGhostTetromino();
                        repaint();
                    }
                    if(key == KeyEvent.VK_P){
                        //Pause
                        //Draw something
                        
                        
                        //Get currentCordinates and store that, and the type of piece when restarting the class
                        if(!pauseState){
                            System.out.println("Paused");
                            //Cancel class
                            timer.cancel();
                            pauseState = true;
                        }else{
                            //Recreate the class
                            System.out.println("Unpaused");
                            timer = new Timer();
                            timer.schedule(new FallingShape(), 0, tickRate);
                            pauseState = false;
                        }
                    }
                }
            }
        }
    }
}

public class Tetris {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run(){
                Window wnd = new Window();
            }
        });
    }
    
}