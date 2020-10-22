import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ListModeForm {
    private JTextField botStopLoss;
    static ArrayList<String> uniqueIdsForUnlockedItems = new ArrayList<>();

    JButton startButton, stopButton, exitButton;

    private JPanel listModeFormRootPanel;

    JTextArea logTextArea;
    private JRadioButton onlyOverpriced;
    private JRadioButton overpricedAndDefaultPrice;
    private JRadioButton onlyDefaultPrice;

    JTextField statusTF;
    JComboBox listsCB;
    private JRadioButton onlyUnlocked;
    private JRadioButton lockedAndUnlocked;
    private JRadioButton onlyLocked;
    private JCheckBox allowCustomPriceCB;
    JCheckBox virtOffersCB;
    JRadioButton dotaGame;
    JRadioButton csgoAndDotaGames;
    JRadioButton csgoGame;

    private JFrame jf;

    private Timer t1;
    private Timer t2;
    private Timer t3;
    private static ThirdPartyTools tpt;
    static CsmSocketFullParser csfp;
    static CsmOIDSocket coids;

    private static CsmInfoRechecker cir;
    private static StSubRechecker ssr;
    private static BotTimer bt;
    private static BuyCsmItem bci;
    private CsmSocketWorker csw;

    private static float botBalance = 0.0f;
    private static float bSL = 0.0f;
    private static boolean isReadyToRestart = true;

    private ArrayList<String> stItemsList = new ArrayList<>();
    private ArrayList<Float> stItemsListMaxPrice = new ArrayList<>();

    private int sDays = 0, sHours = 0, sMins = 0, sSecs = 0, sOverallMins = 0;
    private float sOverallBuyVal = 0f;

    ListModeForm() {
        focusableButtonsMustDie();

        tpt = new ThirdPartyTools();
        cir = new CsmInfoRechecker();
        ssr = new StSubRechecker();
        bt = new BotTimer();
        bci = new BuyCsmItem();
        csfp = new CsmSocketFullParser(3, SForm.lmf, 0f, findMaxPVlistMode());
        coids = new CsmOIDSocket();
        csw = new CsmSocketWorker();

        t1 = new Timer();
        t2 = new Timer();
        t3 = new Timer();

        setupForm();
        setupIcon();
        setupButtons();
        showForm();

        logTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                highlight();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                highlight();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                highlight();
            }

            private void highlight() {
                int pos = logTextArea.getText().lastIndexOf("\n") + 1;
                logTextArea.setCaretPosition(pos);
            }
        });

        startButton.setEnabled(true);

        for (String s : SharkListGrabber.userSets) {
            listsCB.addItem(s);
        }
    }

    static void changeBotBalance(Float currBal) {
        botBalance = currBal - bSL;
        System.out.println("Current bot balance: " + botBalance);
    }

    private void setupForm() {
        jf = new JFrame();
        jf.getContentPane().add(listModeFormRootPanel);
        jf.setTitle("CSM Bot   [List mode]");
        jf.setSize(950, 550);
        jf.setLocationRelativeTo(null);
        jf.setResizable(false);
        jf.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        ButtonGroup foBG = new ButtonGroup();
        foBG.add(onlyOverpriced);
        foBG.add(onlyDefaultPrice);
        foBG.add(overpricedAndDefaultPrice);
        onlyDefaultPrice.setSelected(true);

        ButtonGroup foLOCK = new ButtonGroup();
        foLOCK.add(onlyLocked);
        foLOCK.add(onlyUnlocked);
        foLOCK.add(lockedAndUnlocked);
        onlyLocked.setSelected(true);

        ButtonGroup foGAME = new ButtonGroup();
        foGAME.add(csgoGame);
        foGAME.add(dotaGame);
        foGAME.add(csgoAndDotaGames);
        csgoGame.setSelected(true);

        stopButton.setEnabled(false);
        statusTF.setDisabledTextColor(Color.black);
        allowCustomPriceCB.setSelected(true);

        logTextArea.setEnabled(false);
        logTextArea.setForeground(Color.BLACK);
        logTextArea.setDisabledTextColor(Color.BLACK);
        logTextArea.setText("CSM Bot   [List mode]\r\nПриложение успешно запущено!\r\nЗаполните все необходимые данные и запускайте бота.");
    }

    private void setupIcon() {
        tpt.setupIcon("src/img/csmicon.png", jf);
    }

    private void setupButtons() {

        startButton.addActionListener((ActionEvent e) -> {
            SharkListGrabber.fillUserListFromST(listsCB.getSelectedItem().toString(), SeleniumForSharktools.getSessionToken(), SeleniumForSharktools.getXSRFToken(),
                    stItemsList, stItemsListMaxPrice);

            logTextArea.append("\r\n\r\n" +
                    "Выбранный список был успешно загружен, найдено предметов: " +
                    stItemsList.size() +
                    ".");

            if (botStopLoss.getText().contains(","))
                botStopLoss.setText(botStopLoss.getText().replaceAll(",", "."));

            bSL = Float.parseFloat(botStopLoss.getText());
            loadUserBalanceOnce();

            if (botBalance <= 0) {
                JOptionPane.showMessageDialog(jf, "Значение 'Оставить на балансе' должно быть меньше вашего прежнего баланса!");
                return;
            }

            disableSettings();

            csfp = new CsmSocketFullParser(3, SForm.lmf, 0f, findMaxPVlistMode());
            coids = new CsmOIDSocket();

            tpt.startApp(startButton, stopButton);

            csw = new CsmSocketWorker();
            csw.execute();
        });

        stopButton.addActionListener((ActionEvent e) -> {
            sOverallMins = bt.overallMins;
            sDays = bt.days;
            sHours = bt.hours;
            sMins = bt.minutes;
            sSecs = bt.seconds;
            sOverallBuyVal = BuyCsmItem.overallBuyValue;

            resetTasks();
            resetTimers();

            csfp.isStopped = true;
            csfp.isSecondStage = false;
            coids.isStopped = true;

            csw.cancel(true);
            BuyCsmItem.countOfTries = 10;

            enableSettings();
        });

        exitButton.addActionListener((ActionEvent e) -> {
            try {

                TimeUnit.SECONDS.sleep(2);

            } catch (Exception exc) {

                exc.printStackTrace();

            }

            if (SForm.getSeleniumForSharktools().getSharktoolsChromeDriver() != null)
                SForm.getSeleniumForSharktools().getSharktoolsChromeDriver().quit();

            System.exit(0);
        });
    }

    private void showForm() {
        jf.setVisible(true);
    }

    private void loadUserBalanceOnce() {
        try {
            String link = "https://cs.money/user_info";
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");

            if (SeleniumForCsm.getCfclearanceToken() == null) {
                connection.setRequestProperty("Cookie", SeleniumForCsm.getSessionToken() + "; " +
                        SeleniumForCsm.getUserIdToken() + "; " +
                        "type_device=desktop");
            } else {
                connection.setRequestProperty("Cookie", SeleniumForCsm.getSessionToken() + "; " +
                        SeleniumForCsm.getUserIdToken() + "; " +
                        "type_device=desktop; " +
                        SeleniumForCsm.getCfclearanceToken());
            }

            StringBuilder sBuilder = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String newLine;

            while ((newLine = br.readLine()) != null)
                sBuilder.append(newLine);

            String temp = sBuilder.toString();
            temp = temp.substring(temp.indexOf("\"balance\":") + 10);
            temp = temp.substring(0, temp.indexOf(","));
            temp = temp.trim();

            botBalance = Float.parseFloat(temp);
            botBalance -= bSL;
            logTextArea.append("\r\n\r\nТекущий баланс бота: $" + botBalance + ".");

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии загрузки баланса пользователя, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }

    private void resetTasks() {
        tpt.stopApp(t1, t2, t3, csfp, startButton, stopButton);
        cir = new CsmInfoRechecker();
        ssr = new StSubRechecker();
        bt = new BotTimer();
    }

    private void resetTimers() {
        logTextArea.append("\r\n\r\nПриложение остановлено.");
        t1 = new Timer();
        t2 = new Timer();
        t3 = new Timer();
    }

    private float findMaxPVlistMode() {
        float max = 0;

        if (stItemsListMaxPrice.size() > 0) {
            float max1 = 0;

            for (float f : stItemsListMaxPrice)
                if (f > max1)
                    max1 = f;

            if (max1 > max)
                max = max1;
        }

        return max;
    }

    public class CsmSocketWorker extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() {

            /*
                    Turn off/on some elt-s
             */

            BuyCsmItem.overallBuyValue = sOverallBuyVal;
            isReadyToRestart = true;
            t1.schedule(cir, 1000 * 15, 1000 * 60 * 10);
            t2.schedule(ssr, 1000 * 10, 1000 * 60 * 2);
            t3.schedule(bt, 0, 1000);

            logTextArea.append("\r\n\r\n" +
                    "СТАДИЯ 1 (БД Предметов)." +
                    "\r\n" +
                    "Поиск подходящих предметов...");

            System.out.println("Connecting to CSM4OID Wss...\r\n");
            coids.setupAndRunSecondWSC();

            System.out.println("Connecting to CSM Items BD...\r\n");
            System.out.println("Connecting to CSM WSS...\r\n");
            csfp.setupAndRunWSSConnector(SeleniumForCsm.getSessionToken(), SeleniumForCsm.getUserIdToken());

            if (csgoGame.isSelected()) {
                logTextArea.append("\r\n\r\n" +
                        "[CS:GO]");
                csfp.parseCsmDB(stopButton, "730");

            } else if (dotaGame.isSelected()) {
                logTextArea.append("\r\n\r\n" +
                        "[Dota 2]");
                csfp.parseCsmDB(stopButton, "570");

            } else if (csgoAndDotaGames.isSelected()) {
                logTextArea.append("\r\n\r\n" +
                        "[CS:GO]");
                csfp.parseCsmDB(stopButton, "730");

                if (csfp.isStopped)
                    return null;

                logTextArea.append("\r\n\r\n" +
                        "[Dota 2]");
                csfp.parseCsmDB(stopButton, "570");
            }

            if (!csfp.isStopped) {
                logTextArea.append("\r\n\r\n" +
                        "СТАДИЯ 2 (Веб-Сокеты)." +
                        "\r\n" +
                        "Поиск подходящих предметов...");
                csfp.isSecondStage = true;
            }

            return null;
        }
    }

    synchronized void checkDota(String itemName, String itemPrice, String itemId,
                                String itemBotValue, boolean isFromMarket, String customPrice,
                                String itemJsID, String gemsCount) {

        try {

            if (!(SeleniumForCsm.getUserId().equals(SeleniumForSharktools.getUserSteamId64()))) {
                logTextArea.append("\r\n\r\n" +
                        "Заходить на shark.tools и cs.money нужно под одним аккаунтом, остановка программы...");
                stopButton.doClick();
                return;
            }

            if (csfp.isStopped && coids.isStopped)
                return;

            if (allowCustomPriceCB.isSelected()) {
                if (isFromMarket)
                    return;
            }

            if (isFromMarket) itemPrice = customPrice;

            StringBuilder sb = new StringBuilder();

            System.out.println("New ITEM: \r\n" +
                    itemName + ", price: " + itemPrice);

            if (ItemChecker.checkOnEntry(stItemsList, itemName)) {

                if (ItemChecker.checkOnMaxPriceInList(itemPrice, itemName, stItemsList, stItemsListMaxPrice)) {

                                    /*
                                            BUY FUNCTION
                                     */

                    if (botBalance >= Float.parseFloat(itemPrice)) {
                        botBalance -= Float.parseFloat(itemPrice);

                        sb.append("Trying to buy (DotA 2): ").append(itemName).append("; ")
                                .append("%; ").append(itemPrice).append("$.");
                        logTextArea.append("\r\n\r\n");
                        logTextArea.append(sb.toString());

                        bci.buy(itemName, itemPrice, itemId,
                                null, itemBotValue, null, logTextArea,
                                isFromMarket, customPrice, statusTF, itemJsID, "570", gemsCount);
                    } else {
                        if (botBalance < 0.1f) {
                            logTextArea.append("\r\n" +
                                    "Недостаточно баланса для дальнейшей работы!");
                            if (!csfp.isStopped)
                                stopButton.doClick();
                        }
                    }

                } else {
                    System.out.println("Mismatch by MAXPRICE in user list!");
                }

            } else {
                System.out.println("Miss in user list!");
            }

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии проверки одного из предметов CSM (dota 2), остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }

    synchronized void check(String itemName, String itemPrice, String itemId, String itemHoldTime,
                            String itemBotValue, String itemFloat,
                            boolean isLocked, boolean isOverpaid,
                            boolean isFromMarket, String customPrice, String itemJsID) {
        try {

            if (!(SeleniumForCsm.getUserId().equals(SeleniumForSharktools.getUserSteamId64()))) {
                logTextArea.append("\r\n\r\n" +
                        "Заходить на shark.tools и cs.money нужно под одним аккаунтом, остановка программы...");
                stopButton.doClick();
                return;
            }

            if (csfp.isStopped && coids.isStopped)
                return;

            if (allowCustomPriceCB.isSelected()) {
                if (isFromMarket)
                    return;
            }

            if (isFromMarket) itemPrice = customPrice;

            StringBuilder sb = new StringBuilder();

            System.out.println("New ITEM: \r\n" +
                    itemName + ", price: " + itemPrice);

            if (!ItemChecker.checkOnLockAndOverpayment(onlyLocked, onlyUnlocked, isLocked))
                return;

            if (ItemChecker.checkOnEntry(stItemsList, itemName)) {

                if (ItemChecker.checkOnMaxPriceInList(itemPrice, itemName, stItemsList, stItemsListMaxPrice)) {

                    if (ItemChecker.checkOnLockAndOverpayment(onlyOverpriced, onlyDefaultPrice, isOverpaid)) {

                        if (ItemChecker.checkOnLockAndOverpayment(onlyLocked, onlyUnlocked, isLocked)) {

                        /*
                                BUY FUNCTION
                         */

                            if (botBalance >= Float.parseFloat(itemPrice)) {
                                botBalance -= Float.parseFloat(itemPrice);

                                sb.append("Trying to buy: ").append(itemName).append("; price: ").append(itemPrice).append("$.");
                                logTextArea.append("\r\n\r\n");
                                logTextArea.append(sb.toString());

                                bci.buy(itemName, itemPrice, itemId,
                                        itemHoldTime, itemBotValue, itemFloat, logTextArea,
                                        isFromMarket, customPrice, statusTF, itemJsID, "730", null);
                            } else {
                                if (botBalance < 0.1f) {
                                    logTextArea.append("\r\nНедостаточно баланса для дальнейшей работы!");
                                    if (!csfp.isStopped)
                                        stopButton.doClick();
                                }
                            }

                        } else {
                            System.out.println("Mismatch by lock option.");
                        }

                    } else {
                        System.out.println("Mismatch by overprice option.");
                    }

                } else {
                    System.out.println("Mismatch by maxprice option.");
                }

            } else {
                System.out.println("Miss in user list.");
            }

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии проверки одного из предметов CSM, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }

    void rt() {
        if (isReadyToRestart) {
            isReadyToRestart = false;
            new Timer().schedule(new RestartBot(), 7000);
        }
    }

    class RestartBot extends TimerTask {
        @Override
        public void run() {
            try {

                stopButton.doClick();
                TimeUnit.SECONDS.sleep(8);
                startButton.doClick();

            } catch (Exception exc) {

                SForm.lmf.logTextArea.append("\r\n\r\n" +
                        "Ошибка на стадии перезапуска, остановка программы...");
                exc.printStackTrace();
                SForm.lmf.stopButton.doClick();

            }
        }
    }

    class CsmInfoRechecker extends TimerTask {

        @Override
        public void run() {
            tpt.refreshCsmINFO();
        }
    }

    class StSubRechecker extends TimerTask {
        @Override
        public void run() {
            if (!tpt.checkSharkSub()) {
                try {
                    TimeUnit.SECONDS.sleep(15);
                    if (!tpt.checkSharkSub()) {
                        stopButton.doClick();
                        JOptionPane.showMessageDialog(jf, "Подписка на shark.tools к данному боту отсутствует.");
                        System.exit(0);
                    }
                } catch (InterruptedException exc) {

                    SForm.lmf.logTextArea.append("\r\n\r\n" +
                            "Прерывание сна (повторная остановка программы)...");
                    exc.printStackTrace();
                    SForm.lmf.stopButton.doClick();

                }
            }
        }
    }

    class BotTimer extends TimerTask {
        int days = sDays;
        int hours = sHours;
        int minutes = sMins;
        int seconds = sSecs;

        int overallMins = sOverallMins;

        @Override
        public void run() {
            seconds++;

            if (seconds >= 60) {
                seconds = 0;
                minutes++;

                if (minutes >= 60) {
                    minutes = 0;
                    hours++;

                    if (hours >= 24) {

                        hours = 0;
                        days++;

                    }

                }

            }

            overallMins = minutes + (hours * 60) + (days * 24 * 60);
            float buyPerMin = BuyCsmItem.overallBuyValue / (overallMins + 1);

            String timeString = days + "/" + formatNum(hours) + ":" + formatNum(minutes) + ":" + formatNum(seconds);
            String tmp = statusTF.getText();
            statusTF.setText(tmp.substring(0, tmp.indexOf(":") + 1) + " " + timeString + " <//> Вывод/мин.: $" + buyPerMin);
        }

        String formatNum(int i) {
            if (String.valueOf(i).length() == 1)
                return "0" + i;
            return String.valueOf(i);
        }
    }

    private void focusableButtonsMustDie() {
        startButton.setFocusable(false);
        stopButton.setFocusable(false);
        exitButton.setFocusable(false);
    }

    private void disableSettings() {
        listsCB.setEnabled(false);
        onlyOverpriced.setEnabled(false);
        onlyDefaultPrice.setEnabled(false);
        overpricedAndDefaultPrice.setEnabled(false);
        onlyLocked.setEnabled(false);
        onlyUnlocked.setEnabled(false);
        lockedAndUnlocked.setEnabled(false);
        botStopLoss.setEnabled(false);
        exitButton.setEnabled(false);
        allowCustomPriceCB.setEnabled(false);
        virtOffersCB.setEnabled(false);
        csgoGame.setEnabled(false);
        dotaGame.setEnabled(false);
        csgoAndDotaGames.setEnabled(false);
    }

    private void enableSettings() {
        listsCB.setEnabled(true);
        onlyOverpriced.setEnabled(true);
        onlyDefaultPrice.setEnabled(true);
        overpricedAndDefaultPrice.setEnabled(true);
        onlyLocked.setEnabled(true);
        onlyUnlocked.setEnabled(true);
        lockedAndUnlocked.setEnabled(true);
        botStopLoss.setEnabled(true);
        exitButton.setEnabled(true);
        allowCustomPriceCB.setEnabled(true);
        virtOffersCB.setEnabled(true);
        csgoGame.setEnabled(true);
        dotaGame.setEnabled(true);
        csgoAndDotaGames.setEnabled(true);
    }
}
