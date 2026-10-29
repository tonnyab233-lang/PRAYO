package com.prayo.music;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 41;
    private final ArrayList<Track> tracks = new ArrayList<>();
    private final ArrayList<Track> filtered = new ArrayList<>();
    private int current = -1;
    private TextView nowTitle, nowArtist, playButton, countLabel;
    private TrackAdapter adapter;
    private EditText search;
    private int white = Color.WHITE, muted = Color.rgb(170,170,178), purple = Color.rgb(180,92,255);

    static class Track {
        long id; String title, artist; Uri uri;
        Track(long id, String title, String artist, Uri uri) { this.id=id; this.title=title; this.artist=artist; this.uri=uri; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(9,9,11));
        getWindow().setNavigationBarColor(Color.rgb(9,9,11));
        buildUi();
        if (hasAudioPermission()) loadMusic(); else requestAudioPermission();
    }

    private boolean hasAudioPermission() {
        if (Build.VERSION.SDK_INT >= 33) return checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQ_AUDIO);
        else if (Build.VERSION.SDK_INT >= 23) requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_AUDIO);
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_AUDIO && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) loadMusic();
        else Toast.makeText(this, "PRAYO necesita permiso para leer tu música", Toast.LENGTH_LONG).show();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(18),dp(20),dp(8)); root.setBackgroundColor(Color.rgb(9,9,11));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("PRAYO", 30, white, true); header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView searchIcon = text("♫", 26, purple, true); header.addView(searchIcon); root.addView(header);
        TextView subtitle = text("Tu música, a tu manera.", 14, muted, false); subtitle.setPadding(0,dp(5),0,dp(15)); root.addView(subtitle);
        search = new EditText(this); search.setSingleLine(true); search.setTextColor(white); search.setHintTextColor(muted); search.setHint("Buscar canciones o artistas"); search.setBackgroundColor(Color.rgb(29,29,33)); search.setPadding(dp(14),0,dp(14),0); root.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));
        countLabel = text("Tu biblioteca", 19, white, true); countLabel.setPadding(0,dp(18),0,dp(8)); root.addView(countLabel);
        ListView list = new ListView(this); list.setDivider(new android.graphics.drawable.ColorDrawable(Color.rgb(39,39,43))); list.setDividerHeight(dp(1)); list.setBackgroundColor(Color.TRANSPARENT); adapter = new TrackAdapter(); list.setAdapter(adapter); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout now = new LinearLayout(this); now.setOrientation(LinearLayout.VERTICAL); now.setPadding(dp(14),dp(12),dp(14),dp(10)); now.setBackground(round(Color.rgb(29,27,33),dp(16)));
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
        nowTitle = text("Elige una canción",15,white,true); nowArtist = text("PRAYO",12,muted,false); labels.addView(nowTitle); labels.addView(nowArtist);
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        Button prev = control("|◀"); prev.setOnClickListener(v -> playRelative(-1)); row.addView(prev);
        playButton = text("▶",25,purple,true); playButton.setGravity(Gravity.CENTER); playButton.setOnClickListener(v -> toggle()); row.addView(playButton, new LinearLayout.LayoutParams(dp(48),dp(42)));
        Button next = control("▶|"); next.setOnClickListener(v -> playRelative(1)); row.addView(next); now.addView(row); root.addView(now,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);
        search.addTextChangedListener(new android.text.TextWatcher(){ public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){ filter(s.toString()); } public void afterTextChanged(android.text.Editable e){} });
    }
    private TextView text(String value,int size,int color,boolean bold){ TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(null,1); return t; }
    private Button control(String value){ Button b=new Button(this); b.setText(value); b.setTextColor(white); b.setTextSize(13); b.setBackgroundColor(Color.TRANSPARENT); return b; }
    private android.graphics.drawable.GradientDrawable round(int color,int radius){ android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+0.5f); }

    private void loadMusic() {
        tracks.clear();
        Uri collection = Build.VERSION.SDK_INT >= 29 ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST};
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        try (Cursor c = getContentResolver().query(collection, projection, selection, null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC")) {
            if(c != null) while(c.moveToNext()) {
                long id=c.getLong(0); String title=c.getString(1); String artist=c.getString(2);
                if(title==null) title="Canción sin título"; if(artist==null || artist.equals("<unknown>")) artist="Artista desconocido";
                tracks.add(new Track(id,title,artist,ContentUris.withAppendedId(collection,id)));
            }
        } catch(Exception e){ Toast.makeText(this,"No se pudo cargar la biblioteca",Toast.LENGTH_SHORT).show(); }
        filter(search == null ? "" : search.getText().toString());
        if(tracks.isEmpty()) countLabel.setText("No se encontró música en tu teléfono");
    }
    private void filter(String q){ filtered.clear(); for(Track t:tracks) if(t.title.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)) || t.artist.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))) filtered.add(t); if(adapter!=null)adapter.notifyDataSetChanged(); if(countLabel!=null && !tracks.isEmpty())countLabel.setText("Canciones  ·  "+filtered.size()); }
    private void playTrack(Track t){ try { PlaybackService.play(this, t.uri, t.title, t.artist); current=tracks.indexOf(t); nowTitle.setText(t.title); nowArtist.setText(t.artist); playButton.setText("Ⅱ"); } catch(Exception e){ Toast.makeText(this,"No se pudo reproducir esta canción",Toast.LENGTH_SHORT).show(); } }
    private void toggle(){ if(current<0){ if(!filtered.isEmpty())playTrack(filtered.get(0)); return;} boolean wasPlaying = PlaybackService.isPlaying(); PlaybackService.toggle(this); playButton.setText(wasPlaying?"▶":"Ⅱ"); }
    private void playRelative(int step){ if(tracks.isEmpty())return; int next=current<0?0:(current+step+tracks.size())%tracks.size(); playTrack(tracks.get(next)); }
    @Override protected void onResume(){ super.onResume(); if(current>=0) playButton.setText(PlaybackService.isPlaying()?"Ⅱ":"▶"); }

    class TrackAdapter extends BaseAdapter {
        public int getCount(){return filtered.size();} public Object getItem(int p){return filtered.get(p);} public long getItemId(int p){return filtered.get(p).id;}
        public View getView(int p,View convert,ViewGroup parent){ Track t=filtered.get(p); LinearLayout row=new LinearLayout(MainActivity.this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(4),dp(11),dp(4),dp(11));
            TextView cover=text("♫",22,purple,true); cover.setGravity(Gravity.CENTER); cover.setBackground(round(Color.rgb(34,29,42),dp(10))); row.addView(cover,new LinearLayout.LayoutParams(dp(48),dp(48)));
            LinearLayout labels=new LinearLayout(MainActivity.this); labels.setOrientation(LinearLayout.VERTICAL); labels.setPadding(dp(12),0,0,0); labels.addView(text(t.title,15,white,true)); labels.addView(text(t.artist,13,muted,false)); row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
            TextView more=text("•••",18,muted,true); row.addView(more); row.setOnClickListener(v->playTrack(t)); return row;
        }
    }
}
