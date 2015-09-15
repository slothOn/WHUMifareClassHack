package com.example.zxc.myapplication;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private NfcAdapter nfc;
    private TextView prompttxt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        prompttxt= (TextView) findViewById(R.id.prompt);
        nfc=NfcAdapter.getDefaultAdapter(this);
        if (nfc==null) {
            prompttxt.setText("不支持NFC");
        }else if (!nfc.isEnabled()){
            prompttxt.setText("NFC未开");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction())){
            Log.w("findnfc","1");
            prompttxt.setText("detect NFC");
            new ParseTask().execute(getIntent());
        }
    }
    public static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    class ParseTask extends AsyncTask<Intent,Integer,String>{

        @Override
        protected String doInBackground(Intent... params) {
            return readFromTag(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result!=null){
                prompttxt.setText(result);
            }else {
                prompttxt.setText("failed");
            }
        }
    }
    public String readFromTag(Intent i){
        Log.d("tag1","ttt");
        String metaInfo="NFC\n";

        byte[][] a;
        String[] skey={"FFFFFFFFFFFF","A0A1A2A3A4A5","D3F7D3F7D3F7","000000000000","A0B0C0D0E0F0",
        "A1B1C1D1E1F1","B0B1B2B3B4B5","4D3A99C351DD","1A982C7E459A","AABBCCDDEEFF","B5FF67CBA951",
        "714C5C886E97","587EE5F9350F","A0478CC39091","533CB6C723F6","24020000DBFD","000012ED12ED",
        "8FD0A4F256E9","EE9BD361B01B","FFzzzzzzzzzz","A0zzzzzzzzzz"};
        boolean auth[]=new boolean[skey.length];
        a=new byte[skey.length][];
        for(int t=0;t<skey.length;t++){
            a[t]=hexStringToBytes(skey[t]);
        }
        Tag nfctag=i.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        for (String tech : nfctag.getTechList()) {
            metaInfo+=tech+"\n";
        }
        NfcA n=NfcA.get(nfctag);
        MifareUltralight mfu=MifareUltralight.get(nfctag);
        MifareClassic mfc= MifareClassic.get(nfctag);

        try {
            mfc.connect();
            int type=mfc.getType();//获取tag类型
            int sectorcount=mfc.getSectorCount();//扇区数

            String typeS = "";
            switch (type)
            {
                case MifareClassic.TYPE_CLASSIC:
                    typeS = "TYPE_CLASSIC";
                    break;
                case MifareClassic.TYPE_PLUS:
                    typeS = "TYPE_PLUS";
                    break;
                case MifareClassic.TYPE_PRO:
                    typeS = "TYPE_PRO";
                    break;
                case MifareClassic.TYPE_UNKNOWN:
                    typeS = "TYPE_UNKNOWN";
                    break;
            }
            metaInfo="card type"+mfc.getType()+"\n扇区数:"+mfc.getSectorCount()
            +"\n存储空间块数"+mfc.getBlockCount()+"\n存储空间大小"+mfc.getSize()+"B\n";

            for (int j=0;j<mfc.getSectorCount();j++){
                //verify permission with KeyA
                for(int m=0;m<a.length;m++){
                    auth[m]=mfc.authenticateSectorWithKeyA(j, a[m]);
                    if(auth[m]){
                        int blocknum=0;
                        int bindex=0;
                        metaInfo+="Sector "+j+" verified";
                        //读取扇区中的块
                        blocknum=mfc.getBlockCountInSector(j);
                        bindex=mfc.sectorToBlock(j);//盘块对应到物理块
                        for (int num=0;num<blocknum;num++){
                            byte[]data= mfc.readBlock(bindex);
                            metaInfo+="Block "+bindex+":"+b2hex(data)+"\n";
                            bindex++;
                        }
                        continue;
                    }

                    else{
                        auth[m]=mfc.authenticateSectorWithKeyB(j, a[m]);
                        if(auth[m]){
                            int blocknum=0;
                            int bindex=0;
                            metaInfo+="Sector "+j+" verified";
                            //读取扇区中的块
                            blocknum=mfc.getBlockCountInSector(j);
                            bindex=mfc.sectorToBlock(j);//盘块对应到物理块
                            for (int num=0;num<blocknum;num++){
                                byte[]data= mfc.readBlock(bindex);
                                metaInfo+="Block "+bindex+":"+b2hex(data)+"\n";
                                bindex++;
                            }
                            continue;
                        }
                    }


                }
            }
                /*
                auth[auth.length-1]=
                        mfc.authenticateSectorWithKeyA(j,MifareClassic.KEY_DEFAULT)||
                        mfc.authenticateSectorWithKeyB(j,MifareClassic.KEY_DEFAULT);
                auth[auth.length-2]=
                        mfc.authenticateSectorWithKeyA(j,MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)||
                        mfc.authenticateSectorWithKeyB(j,MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)||
                        mfc.authenticateSectorWithKeyA(j,MifareClassic.KEY_NFC_FORUM)||
                        mfc.authenticateSectorWithKeyB(j,MifareClassic.KEY_NFC_FORUM);
                */


        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return metaInfo;



        //prompttxt.setText(metaInfo);
    }

    public String b2hex(byte[] src){
        if(src==null||src.length==0){
            return null;
        }
        StringBuilder sbd=new StringBuilder();
        for (int k=0;k<src.length;k++){
            int hnum=src[k]&0xFF;
            String s=Integer.toHexString(hnum);
            if(s.length()<2){
                sbd.append('0');
            }
            sbd.append(hnum);
        }
        return sbd.toString();
    }
}
