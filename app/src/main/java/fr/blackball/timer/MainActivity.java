package fr.blackball.timer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(14, 20, 31), PANEL = Color.rgb(27, 37, 52);
    private static final int FG = Color.WHITE, MUTED = Color.rgb(162, 178, 194);
    private static final int GREEN = Color.rgb(41, 183, 146), BLUE = Color.rgb(65, 120, 210);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private LinearLayout root;
    private TextView timerView, statusView, extView, titleView;
    private Button playButton;
    private String[] names = {"Joueur 1", "Joueur 2"};
    private int[] normal = {45, 60};
    private int[] used = {0, 0};
    private int active = 0, first = 90, extSeconds = 30, extLimit = 1;
    private boolean sound = true, vibration = true;
    private boolean firstShotPending = true, running = false, expired = false;
    private long remainingMs = 90000, deadline;
    private ToneGenerator tone;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            remainingMs = Math.max(0, deadline - SystemClock.elapsedRealtime());
            updateTimer();
            if (remainingMs == 0) {
                running = false; expired = true;
                if (playButton != null) playButton.setText("▶  Démarrer");
                if (statusView != null) statusView.setText("TEMPS ÉCOULÉ");
                signalEnd();
            } else handler.postDelayed(this, 70);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("blackball", MODE_PRIVATE);
        loadSettings();
        try { tone = new ToneGenerator(AudioManager.STREAM_ALARM, 90); } catch (Exception ignored) { }
        remainingMs = first * 1000L;
        showTimer();
    }
    @Override public void onDestroy() {
        handler.removeCallbacks(tick);
        if (tone != null) tone.release();
        super.onDestroy();
    }
    private int dp(float x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String label, int sp, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(label); v.setTextColor(color); v.setTextSize(sp);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private void pad(View v, int a, int b, int c, int d) { v.setPadding(dp(a),dp(b),dp(c),dp(d)); }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private void gap(LinearLayout l, int n) { View v = new View(this); l.addView(v, new LinearLayout.LayoutParams(1,dp(n))); }
    private void screen(String heading) {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root = vertical(); pad(root, 20, 26, 20, 30); scroll.addView(root); setContentView(scroll);
        TextView headingView = text(heading, 24, FG, true); root.addView(headingView); gap(root, 22);
    }
    private Button button(String label, int color, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setTextSize(15); b.setAllCaps(false);
        b.setTextColor(FG); b.setBackgroundTintList(ColorStateList.valueOf(color));
        b.setMinHeight(dp(58)); b.setOnClickListener(v -> action.run()); return b;
    }
    private void addButton(LinearLayout area, String label, int color, Runnable action) {
        area.addView(button(label, color, action), new LinearLayout.LayoutParams(-1,dp(58))); gap(area, 8);
    }
    private void showTimer() {
        screen("🎱  Blackball Timer");
        LinearLayout bar = new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL);
        for (int p=0;p<2;p++) {
            final int idx=p;
            Button player = button(names[p] + (p == active ? "  ●" : ""), p == active ? GREEN : PANEL, () -> switchPlayer(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(62),1); lp.setMargins(dp(2),0,dp(2),0); bar.addView(player,lp);
        }
        root.addView(bar); gap(root, 24);
        LinearLayout panel = vertical(); panel.setGravity(Gravity.CENTER); pad(panel, 18, 28, 18, 30);
        panel.setBackground(bg(PANEL, 24)); root.addView(panel);
        titleView = text(names[active], 24, FG, true); titleView.setGravity(Gravity.CENTER); panel.addView(titleView);
        gap(panel, 12);
        statusView = text(firstShotPending ? "PREMIER COUP APRÈS LA CASSE" : "TEMPS PAR COUP", 12, MUTED, true);
        statusView.setGravity(Gravity.CENTER); panel.addView(statusView);
        timerView = text("", 78, FG, true); timerView.setGravity(Gravity.CENTER); panel.addView(timerView);
        extView = text("", 13, MUTED, false); extView.setGravity(Gravity.CENTER); panel.addView(extView);
        updateTimer(); gap(root, 24);
        playButton = button(running ? "Ⅱ  Pause" : "▶  Démarrer", GREEN, this::toggleTimer);
        root.addView(playButton,new LinearLayout.LayoutParams(-1,dp(68))); gap(root, 9);
        addButton(root, "↻  Coup suivant", BLUE, this::nextShot);
        addButton(root, "＋  Extension", Color.rgb(144, 99, 44), this::extension);
        addButton(root, "⇄  Changer de joueur", PANEL, () -> switchPlayer(1-active));
        gap(root, 12);
        addButton(root, "Nouvelle partie", PANEL, () -> new AlertDialog.Builder(this)
            .setTitle("Nouvelle partie ?").setMessage("Réinitialiser les Extensions et réactiver les 90 secondes après la casse ?")
            .setNegativeButton("Annuler",null).setPositiveButton("Confirmer",(d,w)->newGame()).show());
        addButton(root, "⚙  Paramètres", PANEL, this::showSettings);
    }
    private void updateTimer() {
        if (timerView == null) return;
        int secs = (int)Math.ceil(remainingMs / 1000.0);
        timerView.setText(String.format(Locale.FRANCE, "%02d:%02d", secs/60,secs%60));
        timerView.setTextColor(secs <= 10 ? Color.rgb(255, 111, 100) : FG);
        if (extView != null) extView.setText("Extensions " + names[active] + " : " + used[active] + " / " + extLimit);
    }
    private void toggleTimer() {
        if (running) { freeze(); showTimer(); return; }
        if (remainingMs <= 0) { Toast.makeText(this,"Appuyez sur Coup suivant",Toast.LENGTH_SHORT).show(); return; }
        // Dès le lancement du premier coup, le bonus de 90 s est consommé pour cette partie.
        if (firstShotPending) firstShotPending = false;
        expired = false; running = true;
        deadline = SystemClock.elapsedRealtime() + remainingMs;
        handler.removeCallbacks(tick); handler.post(tick); showTimer();
        if (statusView != null) statusView.setText("CHRONO EN COURS");
    }
    private void freeze() {
        if (running) remainingMs = Math.max(0, deadline - SystemClock.elapsedRealtime());
        running = false; handler.removeCallbacks(tick);
    }
    private void nextShot() {
        freeze(); expired = false;
        // Le premier coup bénéficie de 90 s même si l'on presse « Coup suivant » avant le démarrage.
        remainingMs = (firstShotPending ? first : normal[active]) * 1000L;
        showTimer();
    }
    private void switchPlayer(int player) {
        if (player == active) return;
        freeze(); active = player; expired = false;
        remainingMs = (firstShotPending ? first : normal[active]) * 1000L;
        showTimer();
    }
    private void extension() {
        if (used[active] >= extLimit) { Toast.makeText(this,"Plus d'Extension disponible pour ce joueur",Toast.LENGTH_LONG).show(); return; }
        if (expired || remainingMs <= 0) { Toast.makeText(this,"Extension impossible après la fin du temps",Toast.LENGTH_SHORT).show(); return; }
        used[active]++;
        remainingMs = (running ? Math.max(0,deadline-SystemClock.elapsedRealtime()) : remainingMs) + extSeconds*1000L;
        if (running) deadline = SystemClock.elapsedRealtime() + remainingMs;
        updateTimer();
        Toast.makeText(this,"Extension : +" + extSeconds + " secondes",Toast.LENGTH_SHORT).show();
    }
    private void newGame() {
        freeze(); active=0; used[0]=0;used[1]=0; firstShotPending=true; expired=false;
        remainingMs=first*1000L; showTimer();
    }
    private void signalEnd() {
        if (sound && tone != null) try { tone.startTone(ToneGenerator.TONE_PROP_BEEP2,500); } catch (Exception ignored) {}
        if (vibration) try {
            Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(400,VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) {}
    }
    private EditText field(LinearLayout l, String title, String initial, boolean numeric) {
        TextView label = text(title,14,MUTED,false); l.addView(label);gap(l,5);
        EditText input = new EditText(this); input.setTextColor(FG); input.setHintTextColor(MUTED);
        input.setText(initial); input.setSingleLine(true);
        if (numeric) input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setBackgroundTintList(ColorStateList.valueOf(GREEN));
        l.addView(input,new LinearLayout.LayoutParams(-1,dp(51))); gap(l,17); return input;
    }
    private int valid(EditText field, int min, int max) throws NumberFormatException {
        int v=Integer.parseInt(field.getText().toString().trim());
        if (v < min || v > max) throw new NumberFormatException(); return v;
    }
    private void showSettings() {
        freeze(); screen("⚙  Paramètres");
        TextView hint=text("Durées en secondes. Personnalise chaque joueur avant la partie.",14,MUTED,false);root.addView(hint);gap(root,20);
        EditText n1=field(root,"Nom du joueur 1",names[0],false);
        EditText d1=field(root,"Temps par coup du joueur 1",String.valueOf(normal[0]),true);
        EditText n2=field(root,"Nom du joueur 2",names[1],false);
        EditText d2=field(root,"Temps par coup du joueur 2",String.valueOf(normal[1]),true);
        EditText dFirst=field(root,"Premier coup après la casse",String.valueOf(first),true);
        EditText dExt=field(root,"Durée de l'Extension (temps ajouté)",String.valueOf(extSeconds),true);
        EditText dCount=field(root,"Extensions par joueur et par partie",String.valueOf(extLimit),true);
        CheckBox cbSound=new CheckBox(this);cbSound.setText("Signal sonore en fin de temps");cbSound.setTextColor(FG);cbSound.setChecked(sound);root.addView(cbSound);
        CheckBox cbVib=new CheckBox(this);cbVib.setText("Vibration en fin de temps");cbVib.setTextColor(FG);cbVib.setChecked(vibration);root.addView(cbVib);gap(root,20);
        addButton(root,"Enregistrer les paramètres", GREEN, () -> {
            try {
                int a=valid(d1,1,600),b=valid(d2,1,600),c=valid(dFirst,1,600),e=valid(dExt,1,600),f=valid(dCount,0,20);
                String s1=n1.getText().toString().trim(),s2=n2.getText().toString().trim();
                if(s1.isEmpty()||s2.isEmpty()){Toast.makeText(this,"Renseigne les noms des joueurs",Toast.LENGTH_SHORT).show();return;}
                names[0]=s1;names[1]=s2;normal[0]=a;normal[1]=b;first=c;extSeconds=e;extLimit=f;
                sound=cbSound.isChecked();vibration=cbVib.isChecked();
                prefs.edit().putString("n1",s1).putString("n2",s2).putInt("d1",a).putInt("d2",b)
                    .putInt("first",c).putInt("ext",e).putInt("count",f).putBoolean("sound",sound)
                    .putBoolean("vib",vibration).apply();
                // La durée du coup affiché est actualisée, mais les Extensions consommées restent comptabilisées.
                remainingMs=(firstShotPending?first:normal[active])*1000L;
                showTimer();Toast.makeText(this,"Paramètres enregistrés",Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException ex) { Toast.makeText(this,"Durées : 1 à 600 s ; Extensions : 0 à 20",Toast.LENGTH_LONG).show(); }
        });
        addButton(root,"Retour sans enregistrer",PANEL,this::showTimer);
    }
    private void loadSettings() {
        names[0]=prefs.getString("n1",names[0]); names[1]=prefs.getString("n2",names[1]);
        normal[0]=prefs.getInt("d1",45);normal[1]=prefs.getInt("d2",60);
        first=prefs.getInt("first",90);extSeconds=prefs.getInt("ext",30);extLimit=prefs.getInt("count",1);
        sound=prefs.getBoolean("sound",true);vibration=prefs.getBoolean("vib",true);
    }
    @Override public void onBackPressed() { showTimer(); }
}
