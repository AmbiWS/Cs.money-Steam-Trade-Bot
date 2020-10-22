import com.neovisionaries.ws.client.*;

import java.util.Collections;

class CsmOIDSocket {

    private WebSocket csmOidWs;
    boolean isStopped = false;

    void setupAndRunSecondWSC() {
        try {
            isStopped = false;
            final String csmWsLink = "wss://cs.money/ws";
            csmOidWs = new WebSocketFactory()
                    .createSocket(csmWsLink);

            csmOidWs.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String text) {
                    if (!isStopped) {

                        if (text.contains("step_trade") && text.contains("\"balance\":")) {
                            String temp = text;
                            temp = temp.substring(temp.indexOf("\"balance\":") + 10);
                            temp = temp.substring(0, temp.indexOf(","));
                            Float balVal = Float.parseFloat(temp.trim());
                            ListModeForm.changeBotBalance(balVal);
                        }

                        if (text.contains("step_trade") && text.contains("\"offer_id\"")) {
                            if (!SForm.lmf.virtOffersCB.isSelected()) {
                                String temp = text;
                                temp = temp.substring(temp.indexOf("\"offer_id\"") + 11);
                                String offer_id = temp.substring(0, temp.indexOf(","));

                                String uId = temp.substring(temp.indexOf("\"uniqid\":") + 9);
                                uId = uId.substring(0, uId.indexOf("}"));

                                if (uId.contains(","))
                                    uId = uId.substring(0, uId.indexOf(","));

                                if (Collections.frequency(ListModeForm.uniqueIdsForUnlockedItems, uId) >= 1) return;

                                SeleniumForCsm.setOffer_id(offer_id);
                            }
                        }
                    }
                }

                @Override
                public void onFrame(WebSocket websocket, WebSocketFrame frame) {
                    if (isStopped) {
                        csmOidWs.disconnect();
                    }
                }

                @Override
                public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                    try {
                        if (!isStopped) {
                            System.out.println("Reconnecting to WSS[2]...");
                            csmOidWs = csmOidWs.recreate().connect();
                        }
                    } catch (Exception excp) {

                        SForm.lmf.logTextArea.append("\r\n\r\n" +
                                "Ошибка на стадии реконнекта к WSS CSM[2], остановка программы...");
                        excp.printStackTrace();
                        SForm.lmf.stopButton.doClick();

                    }
                }

                @Override
                public void onStateChanged(WebSocket websocket, WebSocketState newState) {
                    System.out.println("WSS4OID State ~> " + newState.name());
                }
            });

            csmOidWs.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");

            if (SeleniumForCsm.getCfclearanceToken() == null) {
                csmOidWs.addHeader("Cookie", SeleniumForCsm.getSessionToken() + "; " +
                        SeleniumForCsm.getUserIdToken() + "; " +
                        "type_device=desktop");
            } else {
                csmOidWs.addHeader("Cookie", SeleniumForCsm.getSessionToken() + "; " +
                        SeleniumForCsm.getUserIdToken() + "; " +
                        "type_device=desktop; " +
                        SeleniumForCsm.getCfclearanceToken());
            }

            csmOidWs.connect();
        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии подключения к WSS CSM[2], остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }
}
