package com.developer.jonery.doodlz;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.print.PrintHelper;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jonery on 07/10/2016.
 */

public class DoodleView extends View {
    //define se o usuario mexeu o dedo suficiente pra desenhar uma linha
    private static final float TOUCH_TOLERANCE = 10;

    private Bitmap bitmap; //area de desenho pra mostrar ou salvar
    private Canvas bitmapCanvas; //usado pra desenhar no bitmap
    private final Paint paintScreen; //usado pra desenhar o bitmap na tela
    private final Paint paintLine; //usado pra desenhar linhas no bitmap

    //Maps dos caminhos que estao sendo desenhados e pontos desses caminhos
    //O primeiro é pra pegar o ponto que os dedos estão
    private final Map<Integer, Path> pathMap = new HashMap<>();
    //Est mantem aonde começou pra poder desenhar a linha
    private final Map<Integer, Point> previousPointMap = new HashMap<>();

    //este construtor inicializa o doodleView
    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintScreen = new Paint();

        //define os sets iniciais da linha
        paintLine = new Paint();
        paintLine.setAntiAlias(true); //smooth edges
        paintLine.setColor(Color.BLACK); //the initial color
        paintLine.setStyle(Paint.Style.STROKE); //solid line
        paintLine.setStrokeWidth(5); //line width
        paintLine.setStrokeCap(Paint.Cap.ROUND); //rounded ends
    }

    //método que define o tamanho do bitmap, ocorre toda vez que muda o tamanho da tela (rotacao)
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //pega o tamanho do doodleView, e o Bitmap tem config de 1 byte por item
        //alpha, red, green e blue em cada byte
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap); //inicializa o canvas que desenha diretamente no bitmap
        bitmap.eraseColor(Color.WHITE); //apaga o bitmap para branco, pois este iniciliza em Black
    }

    //limpa o bitmap
    public void clear(){
        pathMap.clear(); //remove todos os paths
        previousPointMap.clear(); //remove os pontos anteriores
        bitmap.eraseColor(Color.WHITE); //coloca a tela em branco
        invalidate(); //refresh the screen
    }

    //define a cor da linha
    public void setDrawingColor(int color){
        paintLine.setColor(color);
    }

    //retorna a cor da linha
    public int getDrawingColor(){
        return paintLine.getColor();
    }

    //define a grossura da linha
    public void setLineWidth(int width){
        paintLine.setStrokeWidth(width);
    }

    //retorna a grossura da linha
    public int getLineWidth (){
        return (int) paintLine.getStrokeWidth();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //desenha o background
        canvas.drawBitmap(bitmap, 0, 0, paintScreen);

        //desenha cada path que estava desenhado
        for(Integer key : pathMap.keySet())
            canvas.drawPath(pathMap.get(key), paintLine); //pega as caracteristicas da linha
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getActionMasked(); //o tipo do evento, é gerado um int comparando com constante abaixo
        int actionIndex = event.getActionIndex(); //pega o ID do touch, pointer

            //determina se o toque começou, terminou ou esta se movendo
            //o primeiro dedo gera ACTION_DOWN e todos os outros geram ACTION_POINTER_DOWN
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                //pega o ponto inicial do toque - coloquei o i no lugar de actionIndex pra testar o erro e no deu
                touchStarted(event.getX(actionIndex), event.getY(actionIndex), event.getPointerId(actionIndex));
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                //pega o ponto final do toque
                touchEnded(event.getPointerId(actionIndex));
            } else {
                //desenha enquanto nao tirou ou colocou novo dedo
                touchMoved(event);
            }

            invalidate(); //redraw
        return true; //retorna que o evento foi processado
    }

    //chamado quando user toca a tela
    private void touchStarted (float x, float y, int lineID){
        Path path; //usado pra gravar o path pro toque
        Point point; //usado pra gravar o ultimo ponto do toque

        //se ja existe um path pro toque
        if(pathMap.containsKey(lineID)){
            path = pathMap.get(lineID); //pega o path
            path.reset(); //reseta ja que um novo toque foi registrado
            point = previousPointMap.get(lineID); //pega o ultimo ponto
        }
        else {
            path = new Path();
            pathMap.put(lineID, path); //adiciona o path pro Map
            point = new Point();
            previousPointMap.put(lineID, point); // adiciona o ponto ao Map
        }

        //move pras coordenadas do toque
        path.moveTo(x, y);
        point.x = (int) x;
        point.y = (int) y;
    }

    //chamado quando o user move o dedo na tela
    private void touchMoved (MotionEvent event){
        //pra cada pointer
        try {
            for (int i = 0; i < event.getPointerCount(); i++) {
                //pega o ID e index
                int pointerID = event.getPointerId(i);
                int pointerIndex = event.findPointerIndex(i);

                //se tiver um path com o pointer
                if (pathMap.containsKey(pointerID)) {
                    //pega as novas coordenadas para o path
                    float newX = event.getX(pointerIndex);
                    float newY = event.getY(pointerIndex);

                    //pega o path e ponto anterior
                    Path path = pathMap.get(pointerID);
                    Point point = previousPointMap.get(pointerID);

                    //calcula quando o user deslizou o dedo
                    float deltaX = Math.abs(newX - point.x);
                    float deltaY = Math.abs(newY - point.y);

                    //se a distancia é significante
                    if (deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE) {
                        //move o path para a nova localizacao
                        path.quadTo(point.x, point.y, (newX + point.x) / 2, (newY + point.y) / 2);

                        //guarda as novas coordenadas
                        point.x = (int) newX;
                        point.y = (int) newY;
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    //chamada quando tira o dedo
    private void touchEnded(int lineID){
        Path path = pathMap.get(lineID); //pega o path correspondente
        bitmapCanvas.drawPath(path, paintLine);//desenha no bitmapCanvas
        path.reset(); //reseta o path
    }

    //salva a image na galeria
    public void saveImage(){
        //usa o nome do app seguido pelo horario pra salvar
        final String name = "Doodlz" + System.currentTimeMillis() + ".jpg";
         //insere imagem no device
        String location = MediaStore.Images.Media.insertImage(getContext().getContentResolver(),
                bitmap, name, "Doodlz drawing");

        //To solve problem with Date follow commented code below

       /* public static final String insertImage(ContentResolver cr,
                Bitmap source,
                String title,
                String description) {

            ContentValues values = new ContentValues();
            values.put(Images.Media.TITLE, title);
            values.put(Images.Media.DISPLAY_NAME, title);
            values.put(Images.Media.DESCRIPTION, description);
            values.put(Images.Media.MIME_TYPE, "image/jpeg");
            // Add the date meta data to ensure the image is added at the front of the gallery
            values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
            values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());

            Uri url = null;
            String stringUrl = null;    // value to be returned

            try {
                url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (source != null) {
                    OutputStream imageOut = cr.openOutputStream(url);
                    try {
                        source.compress(Bitmap.CompressFormat.JPEG, 50, imageOut);
                    } finally {
                        imageOut.close();
                    }

                    long id = ContentUris.parseId(url);
                    // Wait until MINI_KIND thumbnail is generated.
                    Bitmap miniThumb = Images.Thumbnails.getThumbnail(cr, id, Images.Thumbnails.MINI_KIND, null);
                    // This is for backward compatibility.
                    storeThumbnail(cr, miniThumb, id, 50F, 50F,Images.Thumbnails.MICRO_KIND);
                } else {
                    cr.delete(url, null, null);
                    url = null;
                }
            } catch (Exception e) {
                if (url != null) {
                    cr.delete(url, null, null);
                    url = null;
                }
            }

            if (url != null) {
                stringUrl = url.toString();
            }

            return stringUrl;
        }

        *//**
         * A copy of the Android internals StoreThumbnail method, it used with the insertImage to
         * populate the android.provider.MediaStore.Images.Media#insertImage with all the correct
         * meta data. The StoreThumbnail method is private so it must be duplicated here.
         * @see android.provider.MediaStore.Images.Media (StoreThumbnail private method)
         *//*
        private static final Bitmap storeThumbnail(
                ContentResolver cr,
                Bitmap source,
        long id,
        float width,
        float height,
        int kind) {

            // create the matrix to scale it
            Matrix matrix = new Matrix();

            float scaleX = width / source.getWidth();
            float scaleY = height / source.getHeight();

            matrix.setScale(scaleX, scaleY);

            Bitmap thumb = Bitmap.createBitmap(source, 0, 0,
                    source.getWidth(),
                    source.getHeight(), matrix,
                    true
            );

            ContentValues values = new ContentValues(4);
            values.put(Images.Thumbnails.KIND,kind);
            values.put(Images.Thumbnails.IMAGE_ID,(int)id);
            values.put(Images.Thumbnails.HEIGHT,thumb.getHeight());
            values.put(Images.Thumbnails.WIDTH,thumb.getWidth());

            Uri url = cr.insert(Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

            try {
                OutputStream thumbOut = cr.openOutputStream(url);
                thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
                thumbOut.close();
                return thumb;
            } catch (FileNotFoundException ex) {
                return null;
            } catch (IOException ex) {
                return null;
            }
        }
    */
        if(location!= null){
            //mostra um texto que a imagem foi salva
            Toast message = Toast.makeText(getContext(),
                    R.string.message_saved, Toast.LENGTH_SHORT);

            message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
            message.show();
        }
        else{//mensagem que deu erro
            Toast message = Toast.makeText(getContext(),
                    R.string.message_error_saving, Toast.LENGTH_SHORT);

            message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
            message.show();
        }
    }

    //para imprimir a imagem ou criar pdf
    public void printImage(){
        if(PrintHelper.systemSupportsPrint()){
            //usa a biblioteca do PrintHelper para imprimir a imagem
            PrintHelper printHelper = new PrintHelper(getContext());

            //encaixa a imagem nos limites e imprime
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printHelper.printBitmap("Doodlz Image", bitmap);
        }
        else {//mostra imagem que o sistema nao suporta
            Toast message = Toast.makeText(getContext(),
                    R.string.message_error_printing, Toast.LENGTH_SHORT);

            message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
            message.show();
        }
    }
}
