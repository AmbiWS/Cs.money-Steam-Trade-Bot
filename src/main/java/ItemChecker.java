import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;

class ItemChecker {

    static boolean checkOnEntry(ArrayList<String> anyItemsList, String itemName) {

        return Collections.frequency(anyItemsList, itemName) > 0;
    }

    static boolean checkOnLockAndOverpayment(JRadioButton rb1, JRadioButton rb2, boolean isLocked_isOverpaid) {

        if (rb1.isSelected()) {
            return isLocked_isOverpaid;
        } else if (rb2.isSelected()) {
            return !isLocked_isOverpaid;
        } else return true;
    }

    static boolean checkOnMaxPriceInList(String itemPrice, String itemName,
                                         ArrayList<String> listOfNames, ArrayList<Float> listOfPrices) {

        return Float.parseFloat(itemPrice) <= listOfPrices.get(listOfNames.indexOf(itemName));
    }
}
