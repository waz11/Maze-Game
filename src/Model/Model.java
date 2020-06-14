package Model;

import Client.*;
import IO.MyDecompressorInputStream;
import Server.*;
import algorithms.mazeGenerators.EmptyMazeGenerator;
import algorithms.mazeGenerators.IMazeGenerator;
import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import javafx.scene.input.KeyCode;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Model extends Observable implements IModel {

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private int player_row = 0;
    private int player_col = 0;

    private int goal_row = 0;
    private int goal_col = 1;

    private Server severGenerate;
    private Server serverSolve;

    private boolean isGameOver=false;

    private Maze maze;

    public Model() {
        severGenerate = new Server(5400, 1000, new ServerStrategyGenerateMaze());
        serverSolve = new Server(5401, 1000, new ServerStrategySolveSearchProblem());
    }

    public void startServers() {
        severGenerate.start();
        serverSolve.start();
    }

    public void stopServers() {
        severGenerate.stop();
        serverSolve.stop();
    }

//    private int rows = maze.length;
//    private int cols = maze[0].length;

    @Override
    public void generateMaze(int rows, int cols) {
        //Generate maze
        threadPool.execute(() -> {
            getMazeFromServer(rows, cols);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setChanged();
            notifyObservers();
        });
    }


    private Maze getMazeFromServer(int rows, int cols) {
        try {
            Client client = new Client(InetAddress.getLocalHost(), 5400, new IClientStrategy() {
                public void clientStrategy(InputStream inFromServer, OutputStream outToServer) {
                    try {
                        ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                        ObjectInputStream fromServer = new ObjectInputStream(inFromServer);
                        toServer.flush();

                        int[] mazeDimensions = new int[]{rows,cols};
                        toServer.writeObject(mazeDimensions); //send maze dimensions to server
                        toServer.flush();

                        byte[] compressedMaze = (byte[]) fromServer.readObject(); //read generated maze (compressed with MyCompressor) from server
                        InputStream is = new MyDecompressorInputStream(new ByteArrayInputStream(compressedMaze));
                        byte[] decompressedMaze = new byte[rows*cols+12 /*CHANGE SIZE ACCORDING TO YOU MAZE SIZE*/]; //allocating byte[] for the decompressed maze -
                        is.read(decompressedMaze); //Fill decompressedMaze with bytes
                        maze = new Maze(decompressedMaze);
                        player_row = maze.getStartPosition().getRowIndex();
                        player_col = maze.getStartPosition().getColumnIndex();

                        goal_row = maze.getGoalPosition().getRowIndex();
                        goal_col = maze.getGoalPosition().getColumnIndex();
                        isGameOver = false;

                        toServer.close();
                        fromServer.close();
                    } catch (Exception e) {
                    }
                }
            });
            client.communicateWithServer();
        } catch (UnknownHostException e) {
        }
        return maze;
    }





    @Override
    public Maze getMaze() {
        return maze;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    @Override
    public void moveCharacter(KeyCode movement) {
        if(!isGameOver) {
            int rows = maze.getRows();
            int cols = maze.getCols();
            int row = player_row;
            int col = player_col;
            switch (movement) {
                case UP:
                case NUMPAD8:
                    if (isLegal(player_row - 1, player_col))
                        player_row--;
                    break;
                case DOWN:
                case NUMPAD2:
                    if (isLegal(player_row + 1, player_col))
                        player_row++;
                    break;
                case RIGHT:
                case NUMPAD6:
                    if (isLegal(player_row, player_col + 1))
                        player_col++;
                    break;
                case LEFT:
                case NUMPAD4:
                    if (isLegal(player_row, player_col - 1))
                        player_col--;
                    break;
                case NUMPAD1:
                    if (isLegal(player_row + 1, player_col - 1)) {
                        player_row++;
                        player_col--;
                    }
                    break;
                case NUMPAD3:
                    if (isLegal(player_row + 1, player_col + 1)) {
                        player_row++;
                        player_col++;
                    }
                    break;
                case NUMPAD7:
                    if (isLegal(player_row - 1, player_col - 1)) {
                        player_row--;
                        player_col--;
                    }
                    break;
                case NUMPAD9:
                    if (isLegal(player_row - 1, player_col + 1)) {
                        player_row--;
                        player_col++;
                    }
                    break;
            }


            if (player_row == maze.getGoalPosition().getRowIndex() && player_col == maze.getGoalPosition().getColumnIndex())
                isGameOver = true;
            setChanged();
            notifyObservers();
        }
    }

    @Override
    public int getPlayer_row() {
        return player_row;
    }

    @Override
    public int getPlayer_col() {
        return player_col;
    }

    private boolean isLegal(int row, int col) {
        boolean ans = maze.isLegalPosition(new Position(row, col)) && maze.isPath(new Position(row, col));
        return maze.isLegalPosition(new Position(row, col)) && maze.isPath(new Position(row, col));
    }



    public int getGoal_row() {
        return goal_row;
    }

    public void setGoal_row(int goal_row) {
        this.goal_row = goal_row;
    }

    public int getGoal_col() {
        return goal_col;
    }

    public void setGoal_col(int goal_col) {
        this.goal_col = goal_col;
    }


}
