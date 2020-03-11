package com.example.orxan.xchat;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.HashMap;

public class Contacts {

    public static HashMap getContactList(Context context) {
        HashMap<String,String> contacts = new HashMap<>();

        ContentResolver cr = context.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    try{
                        while (pCur.moveToNext()) {
                            String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            if(phoneNo.length() == 10){
                                phoneNo = "+994".concat(phoneNo.substring(1).trim());
                            }else if(phoneNo.length() == 12){
                                phoneNo = "+".concat(phoneNo.trim());
                            }
                            contacts.put(phoneNo,name);
                        }
                        pCur.close();
                    }catch (NullPointerException e){
                        Log.e("contacts",e.toString());
                    }
                }
            }
        }
        if(cur!=null){
            cur.close();
        }
        return contacts;
    }
}
