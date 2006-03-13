/*
 * Copyright © 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A. All rights reserved.
 * 
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this
 * document. In particular, and without limitation, these intellectual
 * property rights may include one or more of the U.S. patents listed at
 * http://www.sun.com/patents and one or more additional patents or
 * pending patent applications in the U.S. and in other countries.
 * 
 * U.S. Government Rights - Commercial software. Government users are
 * subject to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * 
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 * 
 * This product is covered and controlled by U.S. Export Control laws
 * and may be subject to the export or import laws in other countries.
 * Nuclear, missile, chemical biological weapons or nuclear maritime end
 * uses or end users, whether direct or indirect, are strictly
 * prohibited. Export or reexport to countries subject to U.S. embargo
 * or to entities identified on U.S. export exclusion lists, including,
 * but not limited to, the denied persons and specially designated
 * nationals lists is strictly prohibited.
 * 
 * Copyright © 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, Etats-Unis. Tous droits réservés.
 * 
 * Sun Microsystems, Inc. détient les droits de propriété intellectuels
 * relatifs à la technologie incorporée dans le produit qui est décrit
 * dans ce document. En particulier, et ce sans limitation, ces droits
 * de propriété intellectuelle peuvent inclure un ou plus des brevets
 * américains listés à l'adresse http://www.sun.com/patents et un ou les
 * brevets supplémentaires ou les applications de brevet en attente aux
 * Etats - Unis et dans les autres pays.
 * 
 * L'utilisation est soumise aux termes de la Licence.
 * 
 * Cette distribution peut comprendre des composants développés par des
 * tierces parties.
 * 
 * Sun, Sun Microsystems, le logo Sun et Java sont des marques de
 * fabrique ou des marques déposées de Sun Microsystems, Inc. aux
 * Etats-Unis et dans d'autres pays.
 * 
 * Ce produit est soumis à la législation américaine en matière de
 * contrôle des exportations et peut être soumis à la règlementation en
 * vigueur dans d'autres pays dans le domaine des exportations et
 * importations. Les utilisations, ou utilisateurs finaux, pour des
 * armes nucléaires,des missiles, des armes biologiques et chimiques ou
 * du nucléaire maritime, directement ou indirectement, sont strictement
 * interdites. Les exportations ou réexportations vers les pays sous
 * embargo américain, ou vers des entités figurant sur les listes
 * d'exclusion d'exportation américaines, y compris, mais de manière non
 * exhaustive, la liste de personnes qui font objet d'un ordre de ne pas
 * participer, d'une façon directe ou indirecte, aux exportations des
 * produits ou des services qui sont régis par la législation américaine
 * en matière de contrôle des exportations et la liste de ressortissants
 * spécifiquement désignés, sont rigoureusement interdites.
 */

package com.sun.gi.apps.mcs.matchmaker.client.test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.sun.gi.apps.mcs.matchmaker.client.FolderDescriptor;
import com.sun.gi.apps.mcs.matchmaker.client.GameDescriptor;
import com.sun.gi.apps.mcs.matchmaker.client.IGameChannel;
import com.sun.gi.apps.mcs.matchmaker.client.IGameChannelListener;
import com.sun.gi.apps.mcs.matchmaker.client.ILobbyChannel;
import com.sun.gi.apps.mcs.matchmaker.client.ILobbyChannelListener;
import com.sun.gi.apps.mcs.matchmaker.client.IMatchMakingClientListener;
import com.sun.gi.apps.mcs.matchmaker.client.LobbyDescriptor;
import com.sun.gi.apps.mcs.matchmaker.client.MatchMakingClient;
import com.sun.gi.comm.discovery.impl.URLDiscoverer;
import com.sun.gi.comm.users.client.ClientConnectionManager;
import com.sun.gi.comm.users.client.impl.ClientConnectionManagerImpl;
import com.sun.gi.utils.SGSUUID;
import com.sun.gi.utils.StatisticalUUID;

/**
 * 
 * <p>Title: MatchMakerClientTestUI</p>
 * 
 * <p>Description: This class is a Swing UI that serves as a test harness for
 * the Match Making client.</p>
 * 
 * @author	Sten Anderson
 * @version 1.0
 */
public class MatchMakerClientTestUI extends JFrame
        implements IMatchMakingClientListener {
	
    private MatchMakingClient mmClient;
    private ClientConnectionManager manager;
    private DefaultMutableTreeNode root;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;
    private HashMap<String, LobbyDescriptor> lobbyMap;
    private JTextArea incomingText;

    private JButton connectButton;
    private JButton joinLobby;
    private JButton joinGame;

    public MatchMakerClientTestUI() {
        super();

        setStatus("Not Connected");

        lobbyMap = new HashMap<String, LobbyDescriptor>();

        JPanel treePanel = doTreeLayout();

        connectButton = new JButton("Connect");
        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (connectButton.getText().equals("Connect")) {
                    connect();
                } else {
                    manager.disconnect();
                }
            }
        });

        joinLobby = new JButton("Join Lobby");
        joinLobby.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (joinLobby.getText().startsWith("Join")) {
	                LobbyDescriptor lobby = getSelectedLobby();
	                if (lobby == null) {
	                    return;
	                }
	                mmClient.joinLobby(lobby.getLobbyID().toByteArray(), null);
            	}
            	else {
            		mmClient.leaveLobby();
            	}
                
            }
        });

        JButton createGame = new JButton("Create Game");
        createGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lobbyPanel.createGame();
            }
        });

        joinGame = new JButton("Join Game");
        joinGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (joinGame.getText().startsWith("Join")) {
	            	GameDescriptor game = lobbyPanel.getSelectedGame();
	                if (game == null) {
	                    return;
	                }
	                mmClient.joinGame(game.getGameID().toByteArray());
                }
                else {
                	mmClient.leaveGame();
                }
            }
        });

        JButton readyButton = new JButton("Ready");
        readyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gamePanel.ready();
            }
        });

        JButton startGameButton = new JButton("Start Game");
        startGameButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gamePanel.startGame();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(joinLobby);
        buttonPanel.add(createGame);
        buttonPanel.add(joinGame);
        buttonPanel.add(readyButton);
        buttonPanel.add(startGameButton);

        JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightPane.setDividerLocation(250);
        rightPane.setTopComponent(lobbyPanel = new LobbyPanel());
        rightPane.setBottomComponent(gamePanel = new GamePanel());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setLeftComponent(treePanel);
        splitPane.setRightComponent(rightPane);

        final JTextField chatField = new JTextField(35);
        JButton sendTextButton = new JButton("Send Text");
        sendTextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lobbyPanel.sendText(chatField.getText());
            }
        });

        JButton sendPrivateTextButton = new JButton("Send Private Text");
        sendPrivateTextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lobbyPanel.sendPrivateText(chatField.getText());
            }
        });

        incomingText = new JTextArea(3, 40);

        JPanel chatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chatPanel.add(chatField);
        chatPanel.add(sendTextButton);
        chatPanel.add(sendPrivateTextButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JScrollPane(incomingText), BorderLayout.NORTH);
        bottomPanel.add(chatPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setBounds(300, 200, 700, 600);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (manager != null) {
                    manager.disconnect();
                }
                System.exit(0);
            }
        });

        setVisible(true);
    }

    private void receiveServerMessage(String message) {
        incomingText.setText(incomingText.getText() + "<Server>: " + message
                + "\n");
    }

    private LobbyDescriptor getSelectedLobby() {
        Object selection = tree.getLastSelectedPathComponent();
        LobbyDescriptor lobby = null;
        if (selection != null && selection instanceof LobbyNode) {
            lobby = ((LobbyNode) selection).getDescriptor();
        }
        return lobby;
    }

    private JPanel doTreeLayout() {
        treeModel = createTreeModel();
        tree = new JTree(treeModel);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getNewLeadSelectionPath();
                if (path != null) {
                    TreeNode node = (TreeNode) path.getLastPathComponent();
                }
            }
        });
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);

        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(tree), BorderLayout.CENTER);

        return p;
    }

    public DefaultTreeModel createTreeModel() {
        root = new DefaultMutableTreeNode("Folders");

        return new DefaultTreeModel(root);
    }

    private SGSUUID createUUID(byte[] bytes) {
        SGSUUID id = null;
        try {
            id = new StatisticalUUID(bytes);
        } catch (InstantiationException ie) {}

        return id;
    }

    private void setStatus(String status) {
        setTitle("Match Maker Client Test: " + status);
    }

    public void connect() {
        try {
            manager = new ClientConnectionManagerImpl(
                    "MatchMaker",
                    new URLDiscoverer(
                            new File("resources/FakeDiscovery.xml").toURI().toURL()));
            mmClient = new MatchMakingClient(manager);
            mmClient.setListener(this);
            String[] classNames = manager.getUserManagerClassNames();
            manager.connect(classNames[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void listedFolder(SGSUUID folderID, FolderDescriptor[] subFolders,
            LobbyDescriptor[] lobbies) {

        DefaultMutableTreeNode node = findFolderNode(folderID, root);
        if (node == null) {
            node = root;
        }

        for (FolderDescriptor f : subFolders) {
            treeModel.insertNodeInto(new FolderNode(f), node,
                    node.getChildCount());
        }
        for (LobbyDescriptor l : lobbies) {
            treeModel.insertNodeInto(new LobbyNode(l), node,
                    node.getChildCount());
            lobbyMap.put(l.getChannelName(), l);
        }
        for (FolderDescriptor f : subFolders) {
            mmClient.listFolder(f.getFolderID().toByteArray());
        }
    }

    private FolderNode findFolderNode(SGSUUID folderID,
            DefaultMutableTreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof FolderNode) {
                FolderNode curNode = (FolderNode) node.getChildAt(i);
                if (curNode.getFolderID().equals(folderID)) {
                    return curNode;
                } else if (curNode.getChildCount() > 0) {
                    FolderNode subFolder = findFolderNode(folderID, curNode);
                    if (subFolder != null) {
                        return subFolder;
                    }
                }
            }
        }
        return null;
    }

    public void foundUserName(String userName, byte[] userID) {
    // TODO Auto-generated method stub

    }

    public void foundUserID(String userName, byte[] userID) {
    // TODO Auto-generated method stub

    }

    public void joinedLobby(ILobbyChannel channel) {
        joinLobby.setText("Leave Lobby");
    	lobbyPanel.setLobby(channel);
    }

    public void joinedGame(IGameChannel channel) {
    	joinGame.setText("Leave Game");
    	gamePanel.setGame(channel, lobbyPanel.gameMap.get(channel.getName()));

    }
    
    public void leftLobby() {
    	joinLobby.setText("Join Lobby");
    	receiveServerMessage("Left Lobby");
    	lobbyPanel.resetLobby();
    }
    
    public void leftGame() {
    	joinGame.setText("Join Game");
    	receiveServerMessage("Left Game");
    	gamePanel.resetGame();
    }

    public void connected(byte[] myID) {
        setStatus("Connected");
        connectButton.setText("Disconnect");
        mmClient.listFolder(null);
    }

    public void disconnected() {
        setStatus("Disconnected");
        connectButton.setText("Connect");
    }
    
    /**
     * Called when some command request encounters an error.
     * 
     * @param message		a message detailing the error condition
     */
    public void error(String message) {
    	receiveServerMessage("<ERROR> " + message);
    }

    public void validationRequest(Callback[] callbacks) {
        for (Callback cb : callbacks) {
            if (cb instanceof NameCallback) {
                String value = JOptionPane.showInputDialog(this,
                        "Enter Username", "guest1");
                ((NameCallback) cb).setName(value);
            }
        }
        mmClient.sendValidationResponse(callbacks);
    }

    public static void main(String[] args) {
        new MatchMakerClientTestUI();
    }

    private class FolderNode extends DefaultMutableTreeNode {

        private FolderDescriptor folder;

        public FolderNode(FolderDescriptor f) {
            folder = f;

        }

        public boolean isLeaf() {
            return false;
        }

        public String toString() {
            return folder.getName();
        }

        public SGSUUID getFolderID() {
            return folder.getFolderID();
        }

    }

    private class LobbyNode extends DefaultMutableTreeNode {

        private LobbyDescriptor lobby;

        public LobbyNode(LobbyDescriptor l) {
            lobby = l;

        }

        public boolean isLeaf() {
            return true;
        }

        public String toString() {
            return lobby.getName();
        }

        public SGSUUID getLobbyID() {
            return lobby.getLobbyID();
        }

        public LobbyDescriptor getDescriptor() {
            return lobby;
        }

    }

    private byte[] stringToByteArray(String str) {
        StringTokenizer tokenizer = new StringTokenizer(str, " ");
        byte[] array = new byte[tokenizer.countTokens()];
        int index = 0;
        while (tokenizer.hasMoreTokens()) {
            array[index] = Byte.parseByte(tokenizer.nextToken());
            index++;
        }
        return array;
    }

    /**
     * Convert a byte array to a String by stringing the contents
     * together. Makes for easy hashing.
     * 
     * @param array
     * @return
     */
    private String byteArrayToString(byte[] array) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            buffer.append(array[i] + " ");
        }
        return buffer.toString();
    }

    private class LobbyPanel extends JPanel implements ILobbyChannelListener {

        private JLabel lobbyName;
        private JLabel lobbyDescription;
        private JLabel numUserLabel;
        private JCheckBox isPasswordProtected;
        private ILobbyChannel channel;
        private int numUsers = 0;
        private int maxUsers = 0;
        private DefaultListModel userListModel;
        private JList userList;
        private JList gameList;
        private DefaultListModel gameListModel;
        private GameParametersTableModel parametersModel;
        private HashMap<String, Object> gameParameters;

        private HashMap<String, String> userMap;

        HashMap<String, GameDescriptor> gameMap;

        LobbyPanel() {
            super(new BorderLayout());

            userMap = new HashMap<String, String>();
            gameMap = new HashMap<String, GameDescriptor>();

            JPanel topPanel = new JPanel();
            topPanel.add(new JLabel("Current Lobby:"));
            topPanel.add(lobbyName = new JLabel());
            topPanel.add(lobbyDescription = new JLabel());

            JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            centerPanel.add(numUserLabel = new JLabel("Users: 0/?"));
            centerPanel.add(isPasswordProtected = new JCheckBox(
                    "Password Protected"));
            isPasswordProtected.setEnabled(false);

            int listHeight = 150;
            JTable gameParametersTable = new JTable(
                    parametersModel = new GameParametersTableModel());
            JScrollPane tablePane = new JScrollPane(gameParametersTable);
            tablePane.setPreferredSize(new Dimension(200, listHeight));

            JPanel gameParametersPanel = new JPanel(new BorderLayout());
            gameParametersPanel.add(new JLabel("Game Params"),
                    BorderLayout.NORTH);
            gameParametersPanel.add(tablePane, BorderLayout.CENTER);

            JScrollPane gameListPane = new JScrollPane(gameList = new JList(
                    gameListModel = new DefaultListModel()));
            gameListPane.setPreferredSize(new Dimension(150, listHeight));

            JPanel gameListPanel = new JPanel(new BorderLayout());
            gameListPanel.add(new JLabel("Game List"), BorderLayout.NORTH);
            gameListPanel.add(gameListPane, BorderLayout.CENTER);

            JScrollPane userListPane = new JScrollPane(userList = new JList(
                    userListModel = new DefaultListModel()));
            userListPane.setPreferredSize(new Dimension(150, listHeight));

            JPanel userListPanel = new JPanel(new BorderLayout());
            userListPanel.add(new JLabel("User List"), BorderLayout.NORTH);
            userListPanel.add(userListPane, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.add(userListPanel, BorderLayout.WEST);
            bottomPanel.add(gameListPanel, BorderLayout.CENTER);
            bottomPanel.add(gameParametersPanel, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);
            add(centerPanel, BorderLayout.CENTER);
            add(bottomPanel, BorderLayout.SOUTH);
        }

        public GameDescriptor getSelectedGame() {
            return (GameDescriptor) gameList.getSelectedValue();
        }

        void setLobby(ILobbyChannel channel) {
            LobbyPanel.this.channel = channel;
            channel.setListener(LobbyPanel.this);
            LobbyDescriptor lobby = lobbyMap.get(channel.getName());
            lobbyName.setText(lobby.getName());
            lobbyDescription.setText(lobby.getDescription());
            isPasswordProtected.setSelected(lobby.isPasswordProtected());
            maxUsers = lobby.getMaxUsers();
            updateNumUsers();
            channel.requestGameParameters();
        }
        
        void resetLobby() {
        	LobbyPanel.this.channel = null;
        	lobbyName.setText("");
        	lobbyDescription.setText("");
        	isPasswordProtected.setSelected(false);
        	numUsers = 0;
        	maxUsers = 0;
        	updateNumUsers();
        	gameListModel.removeAllElements();
        	userListModel.removeAllElements();
        	userMap.clear();
        	gameMap.clear();
        	parametersModel.clear();
        
        }

        void createGame() {
        	if (channel != null) {
        		channel.createGame("My Game", "My description", null,
                    gameParameters);
        	}
        }

        void sendText(String message) {
            if (channel != null) {
            	channel.sendText(message);
            }
        }

        void sendPrivateText(String message) {
        	if (channel != null) {
        		byte[] userID = lookupUserID((String) userList.getSelectedValue());
        		channel.sendPrivateText(userID, message);
        	}
        }

        private byte[] lookupUserID(String username) {
            Iterator<String> iterator = userMap.keySet().iterator();
            while (iterator.hasNext()) {
                String curID = iterator.next();
                if (username.equals(userMap.get(curID))) {
                    return stringToByteArray(curID);
                }
            }
            return new byte[0];
        }

        private void updateNumUsers() {
            numUserLabel.setText("Users: " + numUsers + "/" + maxUsers);
        }
        
        private void removeGame(GameDescriptor game) {
        	gameMap.remove(game.getChannelName());
        	gameListModel.removeElement(game);
        }
        
        public void playerEntered(byte[] player, String name) {
            receiveServerMessage(name + " Entered Lobby " + lobbyName.getText());
            userMap.put(byteArrayToString(player), name);
            numUsers++;
            updateNumUsers();
            userListModel.addElement(name);
        }

        public void playerLeft(byte[] player) {
            numUsers--;
            updateNumUsers();
            String name = userMap.remove(byteArrayToString(player));
            receiveServerMessage(name + " Left Lobby " + lobbyName.getText());
            userListModel.removeElement(name);
        }

        public void receiveText(byte[] from, String text, boolean wasPrivate) {
            String userName = userMap.get(byteArrayToString(from));
            incomingText.setText(incomingText.getText() + "<Lobby> " + userName
                    + "" + (wasPrivate ? " (privately)" : "") + ": " + text
                    + "\n");
        }

        public void receivedGameParameters(HashMap<String, Object> parameters) {
            gameParameters = parameters;
            Iterator<String> iterator = parameters.keySet().iterator();
            while (iterator.hasNext()) {
                String curKey = iterator.next();
                parametersModel.addParameter(curKey, parameters.get(curKey));
            }
            parametersModel.fireTableDataChanged();
        }

        public void createGameFailed(String name, String reason) {
            receiveServerMessage("LobbyPanel createGameFailed: " + name
                    + " reason " + reason);
        }

        public void gameCreated(GameDescriptor game) {
            gameListModel.addElement(game);
            gameMap.put(game.getChannelName(), game);
            receiveServerMessage("Game Created: " + game.getName());
        }

        public void playerJoinedGame(byte[] gameID, byte[] player) {
            receiveServerMessage("<Lobby> "
                    + userMap.get(byteArrayToString(player)) + " Joined Game ");
        }
        
        public void gameStarted(GameDescriptor game) {
        	receiveServerMessage("<Lobby> Game Started " + game.getName());
        	removeGame(game);
        }
        
        public void gameDeleted(GameDescriptor game) {
        	receiveServerMessage("<Lobby> Game Deleted " + game.getName());
        	removeGame(game);
        }
    }

    private class GameParametersTableModel extends AbstractTableModel {

        private List<String> params;
        private List values;

        GameParametersTableModel() {
            params = new LinkedList<String>();
            values = new LinkedList();
        }

        public void addParameter(String param, Object value) {
            params.add(param);
            values.add(value);
        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return params.size();
        }
        
        public void clear() {
        	params.clear();
        	values.clear();
        	fireTableDataChanged();
        }

        public Object getValueAt(int row, int col) {
            if (col == 0) {
                return params.get(row);
            }
            return values.get(row);
        }

        public String getColumnName(int col) {
            return col == 0 ? "Parameter" : "Value";
        }
    }

    private class GamePanel extends JPanel implements IGameChannelListener {

        private JLabel gameName;
        private JLabel gameDescription;
        private JLabel numUserLabel;
        private JCheckBox isPasswordProtected;
        private IGameChannel channel;
        private GameDescriptor descriptor;
        private HashMap<String, String> userMap;
        private GameUsersTableModel userTableModel;
        private GameParametersTableModel parametersModel;
        private JTable userTable;

        GamePanel() {
            super(new BorderLayout());

            userMap = new HashMap<String, String>();

            JPanel topPanel = new JPanel();
            topPanel.add(new JLabel("Current Game:"));
            topPanel.add(gameName = new JLabel());
            topPanel.add(gameDescription = new JLabel());

            JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            centerPanel.add(numUserLabel = new JLabel("Users: 0/?"));
            centerPanel.add(isPasswordProtected = new JCheckBox(
                    "Password Protected"));
            isPasswordProtected.setEnabled(false);

            int listHeight = 100;
            JTable gameParametersTable = new JTable(
                    parametersModel = new GameParametersTableModel());
            JScrollPane tablePane = new JScrollPane(gameParametersTable);
            tablePane.setPreferredSize(new Dimension(250, listHeight));

            JPanel gameParametersPanel = new JPanel(new BorderLayout());
            gameParametersPanel.add(new JLabel("Game Params"),
                    BorderLayout.NORTH);
            gameParametersPanel.add(tablePane, BorderLayout.CENTER);

            JScrollPane userListPane = new JScrollPane(userTable = new JTable(
                    userTableModel = new GameUsersTableModel()));
            userListPane.setPreferredSize(new Dimension(220, listHeight));

            JPanel userListPanel = new JPanel(new BorderLayout());
            userListPanel.add(new JLabel("User List"), BorderLayout.NORTH);
            userListPanel.add(userListPane, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.add(userListPanel, BorderLayout.WEST);
            bottomPanel.add(gameParametersPanel, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);
            add(centerPanel, BorderLayout.CENTER);
            add(bottomPanel, BorderLayout.SOUTH);
        }

        public void setGame(IGameChannel channel, GameDescriptor descriptor) {
            GamePanel.this.channel = channel;
            GamePanel.this.descriptor = descriptor;
            channel.setListener(GamePanel.this);
            gameName.setText(descriptor.getName() + ", "
                    + descriptor.getDescription());
            updateGameParameters(descriptor.getGameParameters());
        }
        
        public void resetGame() {
        	GamePanel.this.channel = null;
        	GamePanel.this.descriptor = null;
        	gameName.setText("");
        	userMap.clear();
        	userTableModel.clear();
        	parametersModel.clear();
        	isPasswordProtected.setSelected(false);
        }

        public void ready() {
            if (channel != null) {
            	channel.ready(descriptor, true);
            }
        }

        public void startGame() {
        	if (channel != null) {
        		channel.startGame();
        	}
        }

        private void updateGameParameters(HashMap<String, Object> parameters) {
            Iterator<String> iterator = parameters.keySet().iterator();
            while (iterator.hasNext()) {
                String curKey = iterator.next();
                parametersModel.addParameter(curKey, parameters.get(curKey));
            }
            parametersModel.fireTableDataChanged();
        }

        public void playerEntered(byte[] player, String name) {
            receiveServerMessage("<Game Room> " + name + " Entered Game "
                    + gameName.getText());
            userMap.put(byteArrayToString(player), name);
            userTableModel.addUser(name);
        }

        public void playerLeft(byte[] player) {
            String name = userMap.remove(byteArrayToString(player));
            userTableModel.removeUser(name);
            receiveServerMessage("<Game Room> " + name + " Left Game "
                    + gameName.getText());
        }

        public void receiveText(byte[] from, String text, boolean wasPrivate) {
            String userName = userMap.get(byteArrayToString(from));
            incomingText.setText(incomingText.getText() + "<Game Room> "
                    + userName + "" + (wasPrivate ? " (privately)" : "") + ": "
                    + text + "\n");

        }

        public void playerReady(byte[] player, boolean ready) {
            String userName = userMap.get(byteArrayToString(player));
            receiveServerMessage("<Game Room> " + userName + " is "
                    + (ready ? "" : "not ") + "ready");
            userTableModel.updateReady(userName, ready);
        }

        public void startGameFailed(String reason) {
            receiveServerMessage("<Game Room> Start Game Failed: " + reason);

        }

        public void gameStarted(GameDescriptor game) {
            receiveServerMessage("<Game Room> Game Started " + game.getName());
        }

    }

    private class GameUsersTableModel extends AbstractTableModel {

        private List<String> usernames;
        private List<Boolean> readyState;

        GameUsersTableModel() {
            usernames = new LinkedList<String>();
            readyState = new LinkedList<Boolean>();
        }

        public void addUser(String userName) {
            usernames.add(userName);
            readyState.add(false);
            fireTableDataChanged();
        }

        public void removeUser(String userName) {
            int index = usernames.indexOf(userName);
            if (index == -1) {
                return;
            }
            usernames.remove(userName);
            readyState.remove(index);
            fireTableDataChanged();
        }
        
        public void clear() {
        	usernames.clear();
        	readyState.clear();
        	fireTableDataChanged();
        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return usernames.size();
        }

        public void updateReady(String userName, boolean ready) {
            int index = usernames.indexOf(userName);
            if (index == -1) {
                return;
            }
            readyState.set(index, ready);
            fireTableDataChanged();
        }

        public Object getValueAt(int row, int col) {
            if (col == 0) {
                return usernames.get(row);
            }
            return readyState.get(row);
        }

        public String getColumnName(int col) {
            return col == 0 ? "User" : "Ready?";
        }
    }

}
