package com.nexora.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** A local device dashboard. System settings always own changes to the device. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(12, 16, 18);
    private static final int CARD = Color.rgb(24, 30, 32);
    private static final int TEXT = Color.rgb(240, 244, 239);
    private static final int MUTED = Color.rgb(164, 175, 173);
    private static final int LIME = Color.rgb(196, 245, 107);
    private static final int BLUE = Color.rgb(139, 198, 255);
    private static final int ORANGE = Color.rgb(255, 193, 133);
    private static final String[] TABS = {"Visão geral", "Bateria", "Espaço", "Desempenho"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout content, navigation;
    private ScrollView scroll;
    private int tab;
    private DeviceSnapshot snapshot;
    private final Runnable refreshTask = new Runnable() {
        @Override public void run() { refresh(true); handler.postDelayed(this, 30000); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) tab = state.getInt("tab", 0);
        LinearLayout root = column();
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        LinearLayout header = row();
        header.setPadding(dp(22), dp(14), dp(18), dp(10));
        TextView logo = label("N / NEXORA", 19, TEXT, true);
        logo.setLetterSpacing(.15f);
        header.addView(logo, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button about = button("Sobre", false, this::about);
        header.addView(about);
        root.addView(header);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = column();
        content.setPadding(dp(22), dp(14), dp(22), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        navigation = row();
        navigation.setPadding(dp(10), dp(8), dp(10), dp(10));
        navigation.setBackgroundColor(CARD);
        root.addView(navigation);
        setContentView(root);
        root.requestApplyInsets();
    }

    @Override protected void onResume() {
        super.onResume();
        refresh(true);
        handler.postDelayed(refreshTask, 30000);
    }
    @Override protected void onPause() {
        handler.removeCallbacks(refreshTask);
        super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("tab", tab);
        super.onSaveInstanceState(out);
    }

    private void refresh(boolean preserveScroll) {
        int y = preserveScroll ? scroll.getScrollY() : 0;
        snapshot = DeviceSnapshot.read(this);
        content.removeAllViews();
        navigation.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            final int selected = i;
            Button item = button(TABS[i], tab == i, () -> { tab = selected; refresh(false); });
            item.setTextSize(11);
            item.setSelected(tab == i);
            item.setContentDescription(TABS[i] + (tab == i ? ", selecionado" : ""));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(54), 1);
            params.setMargins(dp(2), 0, dp(2), 0);
            navigation.addView(item, params);
        }
        switch (tab) {
            case 1: battery(); break;
            case 2: storage(); break;
            case 3: performance(); break;
            default: overview();
        }
        TextView timestamp = label("Atualizado às " + DateFormat.getTimeInstance(DateFormat.SHORT,
                Locale.forLanguageTag("pt-BR")).format(new Date()) + " · atualização a cada 30 s", 11, MUTED, false);
        timestamp.setPadding(0, dp(22), 0, dp(6));
        content.addView(timestamp);
        content.addView(button("Atualizar agora", false, () -> refresh(true)));
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    private void overview() {
        eyebrow("SEU CELULAR, EM FOCO");
        title("Mais clareza.\nMais controle.");
        paragraph("Acompanhe os recursos do seu aparelho em um só lugar.");
        LinearLayout hero = card();
        TextView heading = label("ENERGIA PARA O SEU DIA", 11, LIME, true);
        heading.setLetterSpacing(.12f);
        hero.addView(heading);
        LinearLayout batteryRow = row();
        LinearLayout description = column();
        description.addView(label("Bateria", 25, TEXT, true));
        description.addView(label(snapshot.charging(), 14, MUTED, false));
        description.addView(label("Economia " + (snapshot.powerSave ? "ativada" : "desativada"), 12, LIME, false));
        batteryRow.addView(description, new LinearLayout.LayoutParams(0, -2, 1));
        batteryRow.addView(new Ring(snapshot.battery, LIME), new LinearLayout.LayoutParams(dp(126), dp(136)));
        hero.addView(batteryRow);
        hero.addView(button("Ver detalhes da bateria  →", false, () -> { tab = 1; refresh(false); }));
        addCard(hero);
        section("Um olhar no sistema");
        resourceCard("Armazenamento", storagePercent(),
                Metrics.bytes(snapshot.freeStorage) + " disponíveis", BLUE, () -> { tab = 2; refresh(false); });
        resourceCard("Memória RAM em uso", memoryPercent(),
                Metrics.bytes(snapshot.freeMemory) + " disponíveis", ORANGE, () -> { tab = 3; refresh(false); });
        section("Pequenas ações, mais controle");
        LinearLayout tip = card();
        tip.addView(label("Um cuidado de cada vez", 18, TEXT, true));
        tip.addView(body(snapshot.battery >= 0 && snapshot.battery <= 20
                ? "A carga está baixa. A economia de bateria pode ajudar a reduzir o consumo em segundo plano."
                : "Revise os apps que você não usa e mantenha espaço disponível. O Android gerencia a memória automaticamente."));
        tip.addView(button("Abrir configurações de apps", false, () -> open(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)));
        addCard(tip);
    }

    private void battery() {
        eyebrow("ENERGIA");
        title("Sua bateria,\nem detalhes.");
        paragraph("Leituras informadas pelo sistema do aparelho.");
        LinearLayout hero = card();
        Ring ring = new Ring(snapshot.battery, LIME);
        LinearLayout.LayoutParams centered = new LinearLayout.LayoutParams(dp(178), dp(178));
        centered.gravity = Gravity.CENTER_HORIZONTAL;
        hero.addView(ring, centered);
        TextView charging = label(snapshot.charging(), 18, TEXT, true);
        charging.setGravity(Gravity.CENTER);
        hero.addView(charging);
        detail(hero, "Saúde reportada", snapshot.health());
        detail(hero, "Temperatura", snapshot.temperature == Integer.MIN_VALUE ? "Indisponível" :
                String.format(Locale.forLanguageTag("pt-BR"), "%.1f °C", snapshot.temperature / 10f));
        detail(hero, "Tensão", snapshot.voltage < 0 ? "Indisponível" :
                String.format(Locale.forLanguageTag("pt-BR"), "%.2f V", snapshot.voltage / 1000f));
        detail(hero, "Economia de bateria", snapshot.powerSave ? "Ativada" : "Desativada");
        addCard(hero);
        content.addView(button("Configurar economia de bateria", true, () -> open(Settings.ACTION_BATTERY_SAVER_SETTINGS)));
        section("Entenda a leitura");
        paragraph("A saúde é o estado reportado pelo Android, não uma medição da capacidade original. Temperatura e tensão podem não estar disponíveis em alguns aparelhos.");
    }

    private void storage() {
        eyebrow("ARMAZENAMENTO");
        title("Espaço para\no que importa.");
        paragraph("Visão do volume interno de dados do Android.");
        LinearLayout box = card();
        box.addView(label(Metrics.bytes(snapshot.freeStorage), 36, BLUE, true));
        box.addView(label("disponíveis no aparelho", 14, MUTED, false));
        meter(box, storagePercent(), BLUE);
        detail(box, "Capacidade do volume", Metrics.bytes(snapshot.totalStorage));
        detail(box, "Espaço ocupado", Metrics.bytes(used(snapshot.totalStorage, snapshot.freeStorage)));
        detail(box, "Ocupação", Metrics.percentage(storagePercent()));
        addCard(box);
        content.addView(button("Gerenciar armazenamento", true, () -> open(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)));
        section("Você escolhe o que fica");
        LinearLayout tip = card();
        tip.addView(label("Revise antes de excluir", 18, TEXT, true));
        tip.addView(body("Use o gerenciador do Android para revisar downloads, vídeos e apps. O NEXORA não apaga arquivos nem acessa suas fotos."));
        tip.addView(button("Revisar aplicativos", false, () -> open(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)));
        addCard(tip);
        paragraph("O volume de dados pode ter uma capacidade diferente da anunciada pelo fabricante. O espaço disponível considera reservas do sistema; não inclui cartões SD ou armazenamento em nuvem.");
    }

    private void performance() {
        eyebrow("DESEMPENHO");
        title("Conheça o ritmo\ndo seu aparelho.");
        paragraph("Memória e estado térmico, sem pontuações artificiais.");
        LinearLayout box = card();
        box.addView(label("MEMÓRIA RAM EM USO", 11, ORANGE, true));
        box.addView(label(Metrics.percentage(memoryPercent()), 42, TEXT, true));
        meter(box, memoryPercent(), ORANGE);
        detail(box, "RAM total", Metrics.bytes(snapshot.totalMemory));
        detail(box, "RAM disponível", Metrics.bytes(snapshot.freeMemory));
        detail(box, "Pressão de memória", snapshot.totalMemory < 0 ? "Indisponível" : snapshot.lowMemory ? "Elevada" : "Normal");
        addCard(box);
        LinearLayout device = card();
        device.addView(label("Seu dispositivo", 18, TEXT, true));
        detail(device, "Modelo", Build.MANUFACTURER + " " + Build.MODEL);
        detail(device, "Android", Build.VERSION.RELEASE);
        detail(device, "Estado térmico", snapshot.thermal);
        addCard(device);
        section("RAM em uso faz parte");
        paragraph("O Android usa RAM para manter apps prontos e libera recursos quando necessário. Memória ocupada, sozinha, não significa lentidão. O NEXORA não encerra processos nem promete acelerar o aparelho.");
        content.addView(button("Gerenciar aplicativos", true, () -> open(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)));
    }

    private int storagePercent() { return Metrics.percent(used(snapshot.totalStorage, snapshot.freeStorage), snapshot.totalStorage); }
    private int memoryPercent() { return Metrics.percent(used(snapshot.totalMemory, snapshot.freeMemory), snapshot.totalMemory); }
    private long used(long total, long free) { return total < 0 || free < 0 ? -1 : Math.max(0, total - free); }

    private void resourceCard(String name, int value, String caption, int accent, Runnable action) {
        LinearLayout box = card();
        LinearLayout top = row();
        top.addView(label(name, 16, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(label(Metrics.percentage(value), 21, accent, true));
        box.addView(top);
        meter(box, value, accent);
        box.addView(label(caption, 13, MUTED, false));
        box.addView(button("Ver detalhes  →", false, action));
        addCard(box);
    }
    private void meter(LinearLayout parent, int value, int color) {
        View track = new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float radius = dp(4);
                paint.setColor(Color.rgb(49, 57, 58));
                canvas.drawRoundRect(0, 0, getWidth(), getHeight(), radius, radius, paint);
                if (value > 0) {
                    paint.setColor(color);
                    canvas.drawRoundRect(0, 0, getWidth() * value / 100f, getHeight(), radius, radius, paint);
                }
            }
        };
        track.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(7));
        params.setMargins(0, dp(18), 0, dp(16));
        parent.addView(track, params);
    }
    private void detail(LinearLayout parent, String name, String value) {
        LinearLayout line = row();
        line.setPadding(0, dp(14), 0, dp(8));
        line.addView(label(name, 13, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        TextView reading = label(value, 13, TEXT, true);
        reading.setGravity(Gravity.END);
        line.addView(reading, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(line);
    }
    private void eyebrow(String value) {
        TextView text = label(value, 10, LIME, true);
        text.setLetterSpacing(.18f);
        content.addView(text);
    }
    private void title(String value) {
        TextView text = label(value, 32, TEXT, true);
        text.setPadding(0, dp(14), 0, dp(8));
        if (Build.VERSION.SDK_INT >= 28) text.setAccessibilityHeading(true);
        content.addView(text);
    }
    private void section(String value) {
        TextView text = label(value, 18, TEXT, true);
        text.setPadding(0, dp(22), 0, dp(16));
        if (Build.VERSION.SDK_INT >= 28) text.setAccessibilityHeading(true);
        content.addView(text);
    }
    private void paragraph(String value) {
        TextView text = body(value);
        text.setPadding(0, 0, 0, dp(20));
        content.addView(text);
    }
    private TextView body(String value) {
        TextView text = label(value, 14, MUTED, false);
        text.setLineSpacing(dp(4), 1);
        text.setPadding(0, dp(8), 0, dp(12));
        return text;
    }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return text;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout card() {
        LinearLayout box = column();
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        box.setBackground(shape(CARD, 22));
        return box;
    }
    private void addCard(LinearLayout box) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(14));
        content.addView(box, params);
    }
    private Button button(String value, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(primary ? BG : LIME);
        button.setMinHeight(dp(48));
        button.setMinimumHeight(dp(48));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(12), dp(8), dp(12), dp(8));
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x337F9970), shape(primary ? LIME : CARD, 12), null));
        button.setOnClickListener(v -> action.run());
        return button;
    }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void open(String action) {
        try { startActivity(new Intent(action)); }
        catch (ActivityNotFoundException | SecurityException exception) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (ActivityNotFoundException | SecurityException unavailable) {
                Toast.makeText(this, "As configurações não estão disponíveis neste aparelho.", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void about() {
        new AlertDialog.Builder(this).setTitle("NEXORA · 0.1.0")
                .setMessage("Seu celular no melhor desempenho.\n\nUm painel para entender bateria, armazenamento e memória.\n\nSeus dados ficam no aparelho. Sem anúncios, rastreadores, conta ou conexão com servidores. Nenhuma permissão especial é solicitada.\n\nAs leituras dependem das informações disponibilizadas pelo Android e pelo fabricante.")
                .setPositiveButton("Entendi", null).show();
    }

    private final class Ring extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int value, accent;
        private final RectF rect = new RectF();
        Ring(int value, int accent) {
            super(MainActivity.this);
            this.value = value;
            this.accent = accent;
            setContentDescription("Bateria: " + (value < 0 ? "indisponível" : value + " por cento"));
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = Math.min(cx, cy) - dp(12);
            rect.set(cx - radius, cy - radius, cx + radius, cy + radius);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(9));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.rgb(49, 60, 49));
            canvas.drawArc(rect, -90, 360, false, paint);
            paint.setColor(accent);
            if (value > 0) canvas.drawArc(rect, -90, value * 3.6f, false, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(29));
            paint.setColor(TEXT);
            canvas.drawText(Metrics.percentage(value), cx, cy - (paint.ascent() + paint.descent()) / 2, paint);
        }
    }
}
