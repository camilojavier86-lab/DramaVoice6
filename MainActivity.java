package com.dramavoice6.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(7,9,13);
    private final int PANEL = Color.rgb(14,20,29);
    private final int PANEL2 = Color.rgb(18,26,37);
    private final int WHITE = Color.rgb(247,249,252);
    private final int MUTED = Color.rgb(156,169,186);
    private final int PURPLE = Color.rgb(140,82,255);
    private final int AQUA = Color.rgb(34,230,214);

    private LinearLayout page;
    private EditText storyInput;
    private List<Scene> scenes = new ArrayList<>();
    private int activeScene = 0;
    private final Map<String, Button> tabs = new LinkedHashMap<>();

    private static final Pattern TAG = Pattern.compile("^\\s*\\[([^\\]\\n]{1,120})\\]\\s*(.*)$");
    private static final Pattern SCENE = Pattern.compile("^\\s*\\[ESCENA\\s*:\\s*(.+?)\\]\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCENE_END = Pattern.compile("^\\s*\\[FIN\\s*ESCENA\\]\\s*$", Pattern.CASE_INSENSITIVE);

    static final class Turn {
        final String speaker, text;
        Turn(String speaker, String text){ this.speaker=speaker; this.text=text; }
    }
    static final class Scene {
        final String name, raw;
        final List<Turn> turns;
        Scene(String name, String raw, List<Turn> turns){ this.name=name; this.raw=raw; this.turns=turns; }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildShell();
        showHistoria();
    }

    private void buildShell() {
        LinearLayout root = column(BG);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout brand = row();
        ImageView icon = new ImageView(this);
        icon.setImageResource(getResources().getIdentifier("dv6_icon","drawable",getPackageName()));
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(64), dp(64));
        ip.setMargins(0,0,dp(12),0);
        brand.addView(icon, ip);

        LinearLayout brandText = column(Color.TRANSPARENT);
        brandText.addView(text("DramaVoice 6",26,WHITE,true));
        brandText.addView(text("Escenas · personajes · diseño sonoro",13,MUTED,false));
        brand.addView(brandText,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(brand);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout tabRow = row();
        for(String n : new String[]{"Historia","Escenas","Reparto","Personajes","Sonidos","Reproductor","Ajustes"}) {
            Button b = tabButton(n);
            tabs.put(n,b);
            tabRow.addView(b);
        }
        hsv.addView(tabRow);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1,-2);
        tp.setMargins(0,dp(12),0,dp(10));
        root.addView(hsv,tp);

        ScrollView scroll = new ScrollView(this);
        page = column(Color.TRANSPARENT);
        page.setPadding(0,0,0,dp(40));
        scroll.addView(page);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        setContentView(root);

        tabs.get("Historia").setOnClickListener(v->showHistoria());
        tabs.get("Escenas").setOnClickListener(v->showEscenas());
        tabs.get("Reparto").setOnClickListener(v->showReparto());
        tabs.get("Personajes").setOnClickListener(v->showPersonajes());
        tabs.get("Sonidos").setOnClickListener(v->showSonidos());
        tabs.get("Reproductor").setOnClickListener(v->showReproductor());
        tabs.get("Ajustes").setOnClickListener(v->showAjustes());
    }

    private void clearPage(String active) {
        page.removeAllViews();
        for(Map.Entry<String,Button> e:tabs.entrySet()){
            e.getValue().setBackgroundColor(e.getKey().equals(active)?PURPLE:PANEL2);
            e.getValue().setTextColor(WHITE);
        }
    }

    private void showHistoria() {
        clearPage("Historia");
        page.addView(title("Historia"));
        page.addView(help("Pega una escena o varias. Usa [ESCENA: nombre], [Narrador] y [Personaje]."));

        storyInput = new EditText(this);
        storyInput.setTextColor(WHITE);
        storyInput.setHintTextColor(MUTED);
        storyInput.setGravity(Gravity.TOP);
        storyInput.setMinLines(14);
        storyInput.setBackgroundColor(PANEL);
        storyInput.setPadding(dp(14),dp(14),dp(14),dp(14));
        storyInput.setHint("[ESCENA: Apartamento]\\n\\n[Narrador]\\nLa noche...\\n\\n[Isaac]\\nYa llegué.\\n\\n[FIN ESCENA]");
        page.addView(storyInput, margin());

        Button analyze = action("Analizar historia");
        analyze.setOnClickListener(v -> {
            scenes = parseScenes(storyInput.getText().toString());
            ensureCharacters(characters(scenes));
            activeScene=0;
            toast("Detectadas "+scenes.size()+" escena(s)");
            showEscenas();
        });
        page.addView(analyze);
    }

    private void showEscenas() {
        clearPage("Escenas");
        page.addView(title("Escenas"));
        if(scenes.isEmpty()){ page.addView(help("Todavía no hay escenas analizadas.")); return; }
        for(int i=0;i<scenes.size();i++){
            Scene s=scenes.get(i);
            LinearLayout c=card();
            c.addView(text(s.name,20,AQUA,true));
            c.addView(text(s.turns.size()+" intervenciones · aprox. "+estimateMinutes(s.raw)+" min",12,MUTED,false));
            Button use=action(i==activeScene?"Escena activa":"Usar escena");
            final int idx=i;
            use.setOnClickListener(v->{activeScene=idx;showReparto();});
            c.addView(use);
            page.addView(c,margin());
        }
    }

    private void showReparto() {
        clearPage("Reparto");
        page.addView(title("Reparto"));
        if(scenes.isEmpty()){page.addView(help("Analiza una historia primero."));return;}
        Scene sc=scenes.get(Math.min(activeScene,scenes.size()-1));
        LinkedHashSet<String> names=new LinkedHashSet<>();
        names.add("Narrador");
        for(Turn t:sc.turns)names.add(t.speaker);
        for(String n:names){
            LinearLayout c=card();
            c.addView(text(n,21,n.equalsIgnoreCase("Narrador")?AQUA:PURPLE,true));
            c.addView(text(n.equalsIgnoreCase("Narrador")?"Narrador · sin retrato":"Personaje persistente · voz local pendiente",12,MUTED,false));
            page.addView(c,margin());
        }
    }

    private void showPersonajes() {
        clearPage("Personajes");
        page.addView(title("Mis personajes"));
        LinkedHashMap<String,JSONObject> map=loadCharacters();
        if(map.isEmpty()){page.addView(help("Aún no hay personajes guardados."));return;}
        for(JSONObject o:map.values()){
            LinearLayout c=card();
            c.addView(text(o.optString("name","Personaje"),21,AQUA,true));
            c.addView(text("Voz: "+o.optString("voice","Sin asignar"),12,MUTED,false));
            c.addView(text("Se reutilizará cuando el mismo nombre vuelva a aparecer.",12,MUTED,false));
            page.addView(c,margin());
        }
    }

    private void showSonidos() {
        clearPage("Sonidos");
        page.addView(title("Diseño sonoro"));
        if(scenes.isEmpty()){page.addView(help("Analiza una historia primero."));return;}
        Scene sc=scenes.get(Math.min(activeScene,scenes.size()-1));
        page.addView(text(sc.name,18,AQUA,true));
        for(String hint:detectSounds(sc.raw)){
            LinearLayout c=card();
            c.addView(text(hint,16,WHITE,true));
            c.addView(text("Pendiente: asignar muestra, intensidad y posición en la línea de tiempo.",12,MUTED,false));
            page.addView(c,margin());
        }
    }

    private void showReproductor() {
        clearPage("Reproductor");
        page.addView(title("Reproductor"));
        if(scenes.isEmpty()){page.addView(help("No hay escena activa."));return;}
        Scene sc=scenes.get(Math.min(activeScene,scenes.size()-1));

        ImageView portrait=new ImageView(this);
        portrait.setImageResource(getResources().getIdentifier("dv6_icon","drawable",getPackageName()));
        portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(270),dp(270));
        pp.gravity=Gravity.CENTER_HORIZONTAL;
        pp.setMargins(0,dp(18),0,dp(18));
        page.addView(portrait,pp);

        page.addView(centerText(sc.name.toUpperCase(Locale.ROOT),22,AQUA,true));
        SeekBar seek=new SeekBar(this);
        seek.setMax(1000);
        page.addView(seek,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout times=row();
        times.addView(text("00:00",12,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));
        TextView total=text("≤ 15:00",12,MUTED,false); total.setGravity(Gravity.END);
        times.addView(total,new LinearLayout.LayoutParams(0,-2,1));
        page.addView(times);

        Button play=action("▶  Preparar reproducción");
        play.setOnClickListener(v->toast("Siguiente etapa: Supertonic 3 + sherpa-onnx"));
        page.addView(play);
    }

    private void showAjustes() {
        clearPage("Ajustes");
        page.addView(title("Ajustes"));
        LinearLayout c=card();
        c.addView(text("Motor local",19,AQUA,true));
        c.addView(text("Supertonic 3 + sherpa-onnx",17,WHITE,true));
        c.addView(text("Objetivo: multivoz español offline y sin cuotas.",13,MUTED,false));
        page.addView(c,margin());

        LinearLayout c2=card();
        c2.addView(text("DramaVoice 6 Android",19,PURPLE,true));
        c2.addView(text("v0.1.1 · proyecto plano para carga móvil",13,MUTED,false));
        page.addView(c2,margin());
    }

    private List<Scene> parseScenes(String raw) {
        String normalized=raw==null?"":raw.replace("\r","");
        List<Scene> out=new ArrayList<>();
        String currentName=null;
        StringBuilder buffer=new StringBuilder();
        for(String line:normalized.split("\n",-1)){
            Matcher sm=SCENE.matcher(line);
            if(sm.matches()){
                flushScene(out,currentName,buffer);
                currentName=sm.group(1).trim();
                continue;
            }
            if(SCENE_END.matcher(line).matches()){
                flushScene(out,currentName,buffer);
                currentName=null;
                continue;
            }
            buffer.append(line).append('\n');
        }
        flushScene(out,currentName,buffer);
        if(out.isEmpty()&&!normalized.trim().isEmpty())out.add(new Scene("Escena 1",normalized.trim(),parseTurns(normalized)));
        return out;
    }

    private void flushScene(List<Scene> out,String name,StringBuilder buffer){
        String content=buffer.toString().trim();
        if(!content.isEmpty()){
            String nm=(name==null||name.isBlank())?"Escena "+(out.size()+1):name;
            out.add(new Scene(nm,content,parseTurns(content)));
        }
        buffer.setLength(0);
    }

    private List<Turn> parseTurns(String raw){
        List<Turn> turns=new ArrayList<>();
        String speaker=null;
        StringBuilder text=new StringBuilder();
        for(String line:raw.replace("\r","").split("\n")){
            Matcher m=TAG.matcher(line);
            if(m.matches()){
                String tag=m.group(1).trim();
                String upper=tag.toUpperCase(Locale.ROOT);
                if(upper.startsWith("ESCENA")||upper.equals("FIN ESCENA")||upper.startsWith("AMBIENTE")||upper.startsWith("SFX"))continue;
                if(speaker!=null&&!text.toString().trim().isEmpty())turns.add(new Turn(speaker,text.toString().trim()));
                speaker=tag.split("\\|",2)[0].trim();
                text.setLength(0);
                String same=m.group(2).trim();
                if(!same.isEmpty())text.append(same);
            }else if(speaker!=null){
                if(text.length()>0)text.append(' ');
                text.append(line.trim());
            }
        }
        if(speaker!=null&&!text.toString().trim().isEmpty())turns.add(new Turn(speaker,text.toString().trim()));
        return turns;
    }

    private LinkedHashSet<String> characters(List<Scene> list){
        LinkedHashSet<String> names=new LinkedHashSet<>();
        for(Scene s:list)for(Turn t:s.turns)if(!t.speaker.equalsIgnoreCase("Narrador"))names.add(t.speaker);
        return names;
    }

    private void ensureCharacters(Collection<String> names){
        LinkedHashMap<String,JSONObject> map=loadCharacters();
        for(String name:names){
            String key=name.trim().toLowerCase(Locale.ROOT);
            if(!map.containsKey(key)){
                JSONObject o=new JSONObject();
                try{o.put("name",name);o.put("voice","Sin asignar");o.put("portrait","");}catch(Exception ignored){}
                map.put(key,o);
            }
        }
        JSONObject all=new JSONObject();
        try{for(Map.Entry<String,JSONObject>e:map.entrySet())all.put(e.getKey(),e.getValue());}catch(Exception ignored){}
        getSharedPreferences("dramavoice6",MODE_PRIVATE).edit().putString("characters",all.toString()).apply();
    }

    private LinkedHashMap<String,JSONObject> loadCharacters(){
        LinkedHashMap<String,JSONObject> result=new LinkedHashMap<>();
        String raw=getSharedPreferences("dramavoice6",MODE_PRIVATE).getString("characters","{}");
        try{
            JSONObject obj=new JSONObject(raw);
            Iterator<String> it=obj.keys();
            while(it.hasNext()){String k=it.next();result.put(k,obj.getJSONObject(k));}
        }catch(Exception ignored){}
        return result;
    }

    private List<String> detectSounds(String raw){
        String t=raw==null?"":raw.toLowerCase(Locale.ROOT);
        ArrayList<String> out=new ArrayList<>();
        if(any(t,"lluvia","tormenta","trueno"))out.add("Ambiente · lluvia / tormenta");
        if(any(t,"ciudad","tráfico","avenida","autos"))out.add("Ambiente · ciudad");
        if(any(t,"mar","olas","playa"))out.add("Ambiente · mar / playa");
        if(any(t,"puerta","tocó la puerta","golpeó la puerta"))out.add("SFX · puerta / golpes");
        if(any(t,"pasos","caminó","pasillo"))out.add("SFX · pasos");
        if(any(t,"celular","teléfono","vibró"))out.add("SFX · celular");
        if(any(t,"respiración","respiró","jadeó","jadeando"))out.add("Vocalización · respiración / jadeo");
        if(any(t,"gemido","gimió","gimieron"))out.add("Vocalización · gemido");
        if(any(t,"quejido","se quejó"))out.add("Vocalización · quejido");
        if(any(t,"suspiro","suspiró"))out.add("Vocalización · suspiro");
        if(out.isEmpty())out.add("Sin sugerencias automáticas");
        return out;
    }

    private boolean any(String t,String... terms){for(String x:terms)if(t.contains(x))return true;return false;}
    private String estimateMinutes(String raw){int words=raw.trim().isEmpty()?0:raw.trim().split("\\s+").length;return String.format(Locale.US,"%.1f",Math.max(.5,words/155.0));}

    private Button tabButton(String label){
        Button b=new Button(this);b.setText(label);b.setTextSize(13);b.setAllCaps(false);
        b.setPadding(dp(14),dp(4),dp(14),dp(4));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(48));p.setMargins(0,0,dp(8),0);b.setLayoutParams(p);return b;
    }
    private Button action(String label){
        Button b=new Button(this);b.setText(label);b.setTextColor(WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundColor(PURPLE);
        LinearLayout.LayoutParams p=margin();p.height=dp(52);b.setLayoutParams(p);return b;
    }
    private LinearLayout card(){LinearLayout c=column(PANEL);c.setPadding(dp(14),dp(14),dp(14),dp(14));return c;}
    private TextView title(String s){return text(s,28,WHITE,true);}
    private TextView help(String s){return text(s,13,MUTED,false);}
    private TextView text(String s,int sp,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);t.setPadding(0,dp(4),0,dp(4));return t;
    }
    private TextView centerText(String s,int sp,int color,boolean bold){TextView t=text(s,sp,color,bold);t.setGravity(Gravity.CENTER);return t;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout column(int color){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setBackgroundColor(color);return l;}
    private LinearLayout.LayoutParams margin(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(10),0,dp(10));return p;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
