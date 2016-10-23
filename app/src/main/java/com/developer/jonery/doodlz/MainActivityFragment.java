package com.developer.jonery.doodlz;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private DoodleView doodleView; //handle touche events and draws
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;
    private boolean dialogOnScreen = false;

    //valor usado pra saber se o user chacoalhou o cel para apagar
    private static final int ACCELERATION_THRESHOLD = 100000;

    //usado pra identificar o pedido de utilizar external storage pra salvar a imagem
    private static final int SAVE_IMAGE_PERMISSION_REQUEST_CODE = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        //este fragmento tem opcoes pro menu
        setHasOptionsMenu(true);

        //pegar a referencia pro doodleView
        doodleView = (DoodleView) view.findViewById(R.id.doodleView);

        //inicializa os valores de aceleracao
        acceleration = 0.00f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;

        return view;
    }

    //só escuta ao acelerometro quando esta come ste fragmento aberto
    @Override
    public void onResume() {
        super.onResume();
        enableAccelerometerListening(); //escuta ao acelerometro
    }

    //ativa o acelerometro
    private void enableAccelerometerListening(){
        //pega o SensorManager
        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        //registra pra escutar o acelerometro
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    //parar de ouvir o acelerometro
    @Override
    public void onPause() {
        super.onPause();
        disableAccelerometerListening(); //para o listening de shake
    }

    private void disableAccelerometerListening(){
        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        sensorManager.unregisterListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

    //evento handler do acelerometro
    private final SensorEventListener sensorEventListener =
            new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {

                    if(!dialogOnScreen){
                        //pega os x, y e z se nao tiver caixa de dialogo na tela
                        float x = event.values[0];
                        float y = event.values[1];
                        float z = event.values[2];

                        //pega a aceleracao anterior
                        lastAcceleration = currentAcceleration;

                        //calcula a nova aceleracao
                        currentAcceleration = x*x + y*y + z*z;

                        //calcula a variacao da aceleracao
                        acceleration = currentAcceleration*(currentAcceleration-lastAcceleration);

                        //apaga se a aceleracao estiver acima de um valor
                        if(acceleration> ACCELERATION_THRESHOLD){
                            confirmErase();
                        }
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            };

    public void confirmErase(){
        EraseImageDialogFragment fragment = new EraseImageDialogFragment();
        fragment.show(getFragmentManager(), "erase dialog");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.doodle_fragment_menu,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.color:
                ColorDialogFragment colorDialog = new ColorDialogFragment();
                colorDialog.show(getFragmentManager(), "color dialog");
                return true;
            case R.id.line_width:
                LineWidthDialogFragment widthDialog = new LineWidthDialogFragment();
                widthDialog.show(getFragmentManager(), "line width dialog");
                return true;
            case R.id.delete_drawing:
                confirmErase();
                return true;
            case R.id.save:
                saveImage();
                return true;
            case R.id.print:
                doodleView.printImage();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //pra salvar a imagem, primeiro verifica se tem permissao
    //se nao tiver permissao requisita mostrando primeiro o porque
    //depois se permitir salva a imagem
    private void saveImage(){
        //verifica a permissao
        if(getContext().checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!=
                PackageManager.PERMISSION_GRANTED){

            //explica pq precisa da permissao
            if(shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                //mostra a mensagem
                builder.setMessage(R.string.permission_explanation);

                //adiciona o botao OK
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                   //pede permissao
                        requestPermissions(new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                SAVE_IMAGE_PERMISSION_REQUEST_CODE);
                    }
                });
                builder.create().show();
            }
            else {
                requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        SAVE_IMAGE_PERMISSION_REQUEST_CODE);
            }
        }
        else { //se o app ja tem essa permissao
            doodleView.saveImage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case SAVE_IMAGE_PERMISSION_REQUEST_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    doodleView.saveImage();
                return;
        }
    }

    //retorna o DoodleView
    public DoodleView getDoodleView(){
        return doodleView;
    }

    public void setDialogOnScreen(boolean visible){
        dialogOnScreen = visible;
    }

}
