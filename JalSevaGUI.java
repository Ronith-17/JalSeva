import java.awt.*;
import java.awt.event.*;

public class JalSevaGUI extends Frame implements KeyListener {
    private CardLayout cardLayout;
    private Panel mainContainerPanel;
    private TextArea statusLogArea;

    public JalSevaGUI() {
        setTitle("JalSeva - Smart Municipal Water Tanker Distribution System");
        setSize(1050, 780);
        setLocationRelativeTo(null);

        // Window Closing Listener using WindowAdapter
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Closing JalSeva Application...");
                dispose();
                System.exit(0);
            }
        });

        // Register KeyListener for keyboard shortcuts (Ctrl+R, F5, F6)
        addKeyListener(this);
        setFocusable(true);

        setLayout(new BorderLayout());

        // --- Top Control Area (Navigation + Demo Buttons) ---
        Panel topContainer = new Panel(new GridLayout(2, 1));

        // 1. Navigation Bar
        Panel navPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        navPanel.setBackground(new Color(30, 41, 59));

        Button btnRequests = createNavButton("1. Requests (Ctrl+R)", new Color(13, 148, 136));
        Button btnTankers = createNavButton("2. Tankers", new Color(14, 165, 233));
        Button btnTrips = createNavButton("3. Trips (F6)", new Color(99, 102, 241));
        Button btnBilling = createNavButton("4. Billing", new Color(168, 85, 247));
        Button btnDispatch = createNavButton("5. Dispatch (F5)", new Color(234, 88, 12));

        navPanel.add(btnRequests);
        navPanel.add(btnTankers);
        navPanel.add(btnTrips);
        navPanel.add(btnBilling);
        navPanel.add(btnDispatch);
        topContainer.add(navPanel);

        // 2. Sample Outputs Demo Toolbar
        Panel sampleOutputsBar = new Panel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        sampleOutputsBar.setBackground(new Color(204, 251, 241));
        
        Label lblDemoTitle = new Label("Click to Test Sample Outputs ->");
        lblDemoTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        sampleOutputsBar.add(lblDemoTitle);

        Button btnDemo1 = new Button("Dispatch Slip");
        Button btnDemo2 = new Button("Ward Report");
        Button btnDemo3 = new Button("Thread Log");
        Button btnDemo4 = new Button("Error Handling");
        Button btnDemo5 = new Button("Backup & Restore");

        sampleOutputsBar.add(btnDemo1);
        sampleOutputsBar.add(btnDemo2);
        sampleOutputsBar.add(btnDemo3);
        sampleOutputsBar.add(btnDemo4);
        sampleOutputsBar.add(btnDemo5);

        topContainer.add(sampleOutputsBar);
        add(topContainer, BorderLayout.NORTH);

        // --- Main Content Container (CardLayout with all 4 Modules) ---
        cardLayout = new CardLayout();
        mainContainerPanel = new Panel(cardLayout);

        mainContainerPanel.add(createRequestsPanel(), "REQUESTS");
        mainContainerPanel.add(createTankersPanel(), "TANKERS");
        mainContainerPanel.add(createTripsPanel(), "TRIPS");
        mainContainerPanel.add(createBillingPanel(), "BILLING");

        add(mainContainerPanel, BorderLayout.CENTER);

        // --- Bottom Activity Console Area ---
        statusLogArea = new TextArea(8, 80);
        statusLogArea.setEditable(false);
        statusLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statusLogArea.setBackground(new Color(15, 23, 42));
        statusLogArea.setForeground(new Color(56, 189, 248));

        Panel logWrapper = new Panel(new BorderLayout());
        Label lblConsole = new Label(" System Activity & Output Console (Shortcuts: Ctrl+R = New Request, F5 = Dispatch, F6 = Trip Status):");
        lblConsole.setFont(new Font("SansSerif", Font.BOLD, 11));
        logWrapper.add(lblConsole, BorderLayout.NORTH);
        logWrapper.add(statusLogArea, BorderLayout.CENTER);
        
        add(logWrapper, BorderLayout.SOUTH);

        // --- Action Listeners ---
        btnRequests.addActionListener(e -> {
            cardLayout.show(mainContainerPanel, "REQUESTS");
            logMessage("Switched to Requests Module.");
        });

        btnTankers.addActionListener(e -> {
            cardLayout.show(mainContainerPanel, "TANKERS");
            logMessage("Switched to Tankers Fleet Module.");
        });

        btnTrips.addActionListener(e -> {
            cardLayout.show(mainContainerPanel, "TRIPS");
            logMessage("Switched to Trips Tracking Module.");
        });

        btnBilling.addActionListener(e -> {
            cardLayout.show(mainContainerPanel, "BILLING");
            logMessage("Switched to Commercial Billing Module.");
        });

        btnDispatch.addActionListener(e -> handleDispatchAction());

        // Sample Output Demo Listeners
        btnDemo1.addActionListener(e -> printSampleOutput1());
        btnDemo2.addActionListener(e -> printSampleOutput2());
        btnDemo3.addActionListener(e -> printSampleOutput3());
        btnDemo4.addActionListener(e -> printSampleOutput4());
        btnDemo5.addActionListener(e -> printSampleOutput5());

        logMessage("JalSeva Control Room Initialized Successfully. All modules and buttons ready.");
    }

    private Button createNavButton(String title, Color bgColor) {
        Button btn = new Button(title);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        return btn;
    }

    // 1. Requests Panel
    private Panel createRequestsPanel() {
        Panel panel = new Panel(new BorderLayout(10, 10));
        panel.setBackground(new Color(241, 245, 249));

        Panel form = new Panel(new GridLayout(5, 2, 10, 10));
        form.add(new Label(" Consumer ID:"));
        TextField txtConsumerId = new TextField();
        form.add(txtConsumerId);

        form.add(new Label(" Consumer Type:"));
        Choice choiceType = new Choice();
        choiceType.add("Hospital (Highest Priority)");
        choiceType.add("Residential (Free Supply)");
        choiceType.add("Commercial (Charged)");
        form.add(choiceType);

        form.add(new Label(" Ward Number:"));
        TextField txtWard = new TextField();
        form.add(txtWard);

        form.add(new Label(" Water Quantity (Litres):"));
        TextField txtLitres = new TextField();
        form.add(txtLitres);

        Button btnSubmitRequest = new Button("Submit Request");
        btnSubmitRequest.setBackground(new Color(13, 148, 136));
        btnSubmitRequest.setForeground(Color.WHITE);
        form.add(btnSubmitRequest);
        
        Button btnClear = new Button("Clear Form");
        btnClear.setBackground(new Color(148, 163, 184));
        btnClear.setForeground(Color.WHITE);
        form.add(btnClear);

        Label titleLbl = new Label("   === Water Distribution Request Registration ===", Label.CENTER);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

        panel.add(titleLbl, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);

        btnSubmitRequest.addActionListener(e -> {
            logMessage("New Request Logged -> Consumer: " + txtConsumerId.getText() + ", Litres: " + txtLitres.getText() + ", Ward: " + txtWard.getText());
            txtConsumerId.setText("");
            txtLitres.setText("");
            txtWard.setText("");
        });

        btnClear.addActionListener(e -> {
            txtConsumerId.setText("");
            txtLitres.setText("");
            txtWard.setText("");
            logMessage("Request form cleared.");
        });

        return panel;
    }

    // 2. Tankers Panel
    private Panel createTankersPanel() {
        Panel panel = new Panel(new BorderLayout(10, 10));
        panel.setBackground(new Color(241, 245, 249));
        
        List tankerList = new List(8, false);
        tankerList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tankerList.add("AP16 TT 4410 | Capacity: 12,000L | Driver: Ramu | Status: AVAILABLE");
        tankerList.add("AP16 TT 4411 | Capacity: 10,000L | Driver: Suresh | Status: ON TRIP");
        tankerList.add("AP16 TT 4412 | Capacity: 15,000L | Driver: Mahesh | Status: AVAILABLE");

        Label titleLbl = new Label("   === Active Tanker Fleet Status ===", Label.CENTER);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        panel.add(titleLbl, BorderLayout.NORTH);
        panel.add(tankerList, BorderLayout.CENTER);
        return panel;
    }

    // 3. Trips Panel (Includes MouseAdapter)
    private Panel createTripsPanel() {
        Panel panel = new Panel(new BorderLayout(10, 10));
        panel.setBackground(new Color(241, 245, 249));
        
        List tripList = new List(8, false);
        tripList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tripList.add("Trip #101 | Request: WR-2208 | Tanker: AP16 TT 4410 | Dispatched: 09:42 AM | Status: On Trip");
        tripList.add("Trip #102 | Request: WR-2209 | Tanker: AP16 TT 4411 | Dispatched: 10:15 AM | Status: Delivered");

        tripList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selectedTrip = tripList.getSelectedItem();
                if (selectedTrip != null) {
                    logMessage("Selected Trip Details: " + selectedTrip);
                }
            }
        });

        Label titleLbl = new Label("   === Municipal Dispatch & Trip Logs (Click item for details) ===", Label.CENTER);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

        panel.add(titleLbl, BorderLayout.NORTH);
        panel.add(tripList, BorderLayout.CENTER);
        return panel;
    }

    // 4. Billing Panel
    private Panel createBillingPanel() {
        Panel panel = new Panel(new BorderLayout(10, 10));
        panel.setBackground(new Color(241, 245, 249));

        TextArea billingArea = new TextArea();
        billingArea.setEditable(false);
        billingArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        billingArea.setText("COMMERCIAL CONSUMER BILLING REPORT\n" +
                            "--------------------------------------------------\n" +
                            "ID     Name                     Litres    Rate/L    Total Amount\n" +
                            "C-101  Hotel Green Park         45,000    Rs. 0.15  Rs. 6,750.00\n" +
                            "C-104  Metro Plaza Mall         80,000    Rs. 0.15  Rs. 12,000.00\n" +
                            "--------------------------------------------------\n" +
                            "Total Commercial Revenue Collected: Rs. 18,750.00");

        Label titleLbl = new Label("   === Commercial Consumer Billing & Invoices ===", Label.CENTER);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

        panel.add(titleLbl, BorderLayout.NORTH);
        panel.add(billingArea, BorderLayout.CENTER);
        return panel;
    }

    // --- Sample Output Generators ---

    private void printSampleOutput1() {
        statusLogArea.append("\n----- DISPATCH SLIP -----\n" +
                             "Request: WR-2208 (Hospital)\n" +
                             "Consumer: Apex Government Hospital, Ward 12\n" +
                             "Litres: 12,000\n" +
                             "Tanker: AP16 TT 4410 (Driver: Ramu)\n" +
                             "Dispatched: 09:42 AM\n" +
                             "Status: On Trip\n" +
                             "-------------------------\n");
    }

    private void printSampleOutput2() {
        statusLogArea.append("\nWARD SUPPLY REPORT\n" +
                             "Date: 02-11-2026\n" +
                             "Trips Completed: 46\n" +
                             "Total Supplied: 4,92,000 litres\n" +
                             "---\n" +
                             "Highest Ward: Ward 7 (62,000 litres)\n" +
                             "Commercial Billing: Rs. 18,450\n" +
                             "Pending Requests: 5\n" +
                             "-------------------\n");
    }

    private void printSampleOutput3() {
        statusLogArea.append("\n--- THREAD ACTIVITY LOG ---\n" +
                             "TankerTripThread | priority 5 | state: RUNNABLE\n" +
                             "DispatchTask | priority 5 | state: RUNNABLE\n" +
                             "DispatchTask | waiting for data | state: WAITING\n" +
                             "TankerTripThread | update complete, notify() called\n" +
                             "DispatchTask | resumed | state: RUNNABLE\n" +
                             "TankerTripThread | finished | state: TERMINATED\n" +
                             "---------------------------\n");
    }

    private void printSampleOutput4() {
        statusLogArea.append("\nERROR MESSAGES\n" +
                             "NoTankerAvailableException: All tankers are on trips\n" +
                             "QuotaExceededException: Request exceeds daily quota\n" +
                             "InvalidWardException: Ward number not served\n" +
                             "NumberFormatException: Please enter a valid number\n" +
                             "--------------\n");
    }

    private void printSampleOutput5() {
        statusLogArea.append("\n--- BACKUP AND RESTORE ---\n" +
                             "File: trips.dat\n" +
                             "Records Written: 25\n" +
                             "Bytes Written: 3,180\n" +
                             "Records Restored: 25\n" +
                             "Verification: All records match\n" +
                             "--------------------------\n");
    }

    private void handleDispatchAction() {
        logMessage("Action Triggered (F5): Dispatching pending hospital/residential requests...");
    }

    private void handleTripStatusAction() {
        cardLayout.show(mainContainerPanel, "TRIPS");
        logMessage("Action Triggered (F6): Displaying live trip status updates.");
    }

    private void logMessage(String msg) {
        statusLogArea.append("[" + java.time.LocalTime.now().withNano(0) + "] " + msg + "\n");
    }

    // KeyListener implementation for shortcuts (Ctrl+R, F5, F6)
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (e.isControlDown() && keyCode == KeyEvent.VK_R) {
            cardLayout.show(mainContainerPanel, "REQUESTS");
            logMessage("Shortcut Triggered [Ctrl+R]: Switched to New Request Form.");
        } else if (keyCode == KeyEvent.VK_F5) {
            handleDispatchAction();
        } else if (keyCode == KeyEvent.VK_F6) {
            handleTripStatusAction();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            JalSevaGUI gui = new JalSevaGUI();
            gui.setVisible(true);
            gui.toFront();
            gui.requestFocus();
        });
    }
}