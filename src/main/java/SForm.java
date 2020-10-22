import javax.swing.*;
import java.awt.event.ActionEvent;

public class SForm {
    private JButton sharktoolsAuthorization, exitButton, sharktoolsSubscribeChecker, csmSaveAuthButton;
    private JPanel sformRootPanel;
    private JFrame jf;
    private static SeleniumForSharktools seleniumForSharktools;
    private static SeleniumForCsm seleniumForCsm;
    private static ThirdPartyTools tpt;
    static ListModeForm lmf;

    static SeleniumForSharktools getSeleniumForSharktools() {
        return seleniumForSharktools;
    }

    static SeleniumForCsm getSeleniumForCsm() {
        return seleniumForCsm;
    }

    SForm() {
        setupForm();
        setupIcon();
        setupButtons();
        showForm();
    }

    private void setupButtons() {
        exitButton.addActionListener((ActionEvent e) -> {
            System.out.println("Closing application...");
            if (seleniumForSharktools != null)
                if (seleniumForSharktools.getSharktoolsChromeDriver() != null)
                    seleniumForSharktools.getSharktoolsChromeDriver().quit();
            System.exit(0);
        });

        sharktoolsAuthorization.addActionListener((ActionEvent e) ->
            seleniumForSharktools = new SeleniumForSharktools());

        csmSaveAuthButton.addActionListener((ActionEvent e) -> {
            seleniumForCsm.loadTokens(seleniumForSharktools.getSharktoolsChromeDriver());
            seleniumForCsm.loadCsrfTokenHeader(seleniumForSharktools.getSharktoolsChromeDriver());

            if (!SeleniumForCsm.getUserIdToken().contains(SeleniumForSharktools.getUserSteamId64())) {
                JOptionPane.showMessageDialog(jf, "Авторизироватся на shark.tools и cs.money необходимо под одним и тем же аккаунтом, выключение программы...");
                seleniumForSharktools.getSharktoolsChromeDriver().quit();
                System.exit(0);
            }

            jf.setVisible(false);
            lmf = new ListModeForm();
        });

        sharktoolsSubscribeChecker.addActionListener((ActionEvent e) ->
                sharktoolsSubscribeCheckFunction());
    }

    private void setupIcon() {
        tpt.setupIcon("src/img/sharkicon.png", jf);
    }

    private void sharktoolsSubscribeCheckFunction() {
        try {
            seleniumForSharktools.loadTokens();

            if (SeleniumForSharktools.getSessionToken() == null || SeleniumForSharktools.getXSRFToken() == null) {

                JOptionPane.showMessageDialog(jf, "Сначала авторизируйся на сайте shark.tools!");
            } else {

                boolean isSubscribed = tpt.checkSharkSub();

                if (isSubscribed) {
                    System.out.println("\nПодписка на CSMoney [List Withdraw] присутствует, выберите режим бота!");
                    if (seleniumForSharktools != null)
                        if (seleniumForSharktools.getSharktoolsChromeDriver() != null) {
                            seleniumForSharktools.getSharktoolsChromeDriver().get("https://shark.tools/profile");
                            seleniumForSharktools.loadSteamId64();

                            SharkListGrabber.loadUserListsSet(SeleniumForSharktools.getSessionToken(), SeleniumForSharktools.getXSRFToken());

                            JOptionPane.showMessageDialog(jf, "Подписка на CSM List Withdraw Присутствует!\r\n" +
                                    "Авторизируйтесь на Cs.Money.");
                            seleniumForSharktools.getSharktoolsChromeDriver().get("https://cs.money/ru");
                            csmSaveAuthButton.setEnabled(true);
                        }
                } else {
                    JOptionPane.showMessageDialog(jf, "Подписка на CSMoney [List Withdraw] отсутствует!");
                }
            }

        } catch (NullPointerException exc) {

            exc.printStackTrace();
            JOptionPane.showMessageDialog(jf, "Сначала авторизируйся на сайте shark.tools!");

        } catch (Exception exc) {

            if (seleniumForSharktools != null)
                if (seleniumForSharktools.getSharktoolsChromeDriver() != null)
                    seleniumForSharktools.getSharktoolsChromeDriver().quit();
            exc.printStackTrace();
            JOptionPane.showMessageDialog(jf, "Ошибка на стадии проверки подписки на бота, выключение программы...");
            System.exit(0);

        }
    }

    private void showForm() {
        jf.setVisible(true);
    }

    private void setupForm() {
        focusableButtonsMustDie();

        jf = new JFrame();
        jf.getContentPane().add(sformRootPanel);
        jf.setTitle("Subscription checker");
        jf.setSize(330, 260);
        jf.setLocationRelativeTo(null);
        jf.setResizable(false);
        jf.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        tpt = new ThirdPartyTools();
        seleniumForCsm = new SeleniumForCsm();
        csmSaveAuthButton.setEnabled(false);
    }

    private void focusableButtonsMustDie() {
        sharktoolsAuthorization.setFocusable(false);
        sharktoolsSubscribeChecker.setFocusable(false);
        csmSaveAuthButton.setFocusable(false);
        exitButton.setFocusable(false);
    }
}
