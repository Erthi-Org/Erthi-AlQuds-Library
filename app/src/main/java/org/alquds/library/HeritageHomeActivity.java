package org.alquds.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.text.InputType;
import android.text.Html;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeritageHomeActivity extends Activity {
    private LibraryView libraryView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(2, 12, 20));
        window.setNavigationBarColor(Color.rgb(2, 12, 20));
        libraryView = new LibraryView();
        setContentView(libraryView);
    }

    @Override
    public void onBackPressed() {
        if (libraryView != null && libraryView.goBack()) return;
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (libraryView != null) libraryView.dispose();
        super.onDestroy();
    }

    private void showHomeMenu(){
        String[] labels={"كتبي وقراءة PDF","الفهرس المحلي","المصادر","عن إرثي"};
        new AlertDialog.Builder(this).setTitle("مكتبة القدس الجديدة").setItems(labels,(dialog,index)->{
            if(index==0) startActivity(new Intent(this,LocalPdfActivity.class));
            else if(index==1) startActivity(new Intent(this,MainActivity.class));
            else if(index==2){libraryView.screen=Screen.SOURCES;libraryView.invalidate();}
            else new AlertDialog.Builder(this).setTitle("إرثي").setMessage("علمٌ نافع، وأثرٌ باقٍ\nمكتبة القدس الجديدة").setPositiveButton("الموقع",(d,w)->openUrl("https://erthi.org")).setNegativeButton("إغلاق",null).show();
        }).setNegativeButton("إغلاق",null).show();
    }

    private void showLibraryMenu(){
        new AlertDialog.Builder(this).setTitle("المكتبة").setItems(new String[]{"كتبي وقراءة PDF","الفهرس المحلي","البحث في المصادر"},(dialog,index)->{
            if(index==0) startActivity(new Intent(this,LocalPdfActivity.class));
            else if(index==1) startActivity(new Intent(this,MainActivity.class));
            else showSearchDialog();
        }).setNegativeButton("إغلاق",null).show();
    }

    private void showSearchDialog() {
        final EditText input = new EditText(this);
        input.setHint("ابحث عن كتاب، مؤلف، أو موضوع...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.rgb(125, 142, 155));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setGravity(Gravity.RIGHT);
        input.setPadding(36, 18, 36, 18);
        new AlertDialog.Builder(this)
                .setTitle("بحث مكتبة القدس الجديدة")
                .setView(input)
                .setPositiveButton("بحث", (dialog, which) -> {
                    String q = input.getText().toString().trim();
                    if (q.isEmpty()) q = "ابن خلدون";
                    libraryView.searchQuery = q;
                    libraryView.activeCategory = "";
                    libraryView.screen = Screen.RESULTS;
                    libraryView.resultsPage = 0;
                    libraryView.fetchUnifiedSources(q);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showSurahDialog(QuranReciter reciter) {
        final String[] surahs = {
                "الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"
        };
        new AlertDialog.Builder(this)
                .setTitle("اختر السورة — " + reciter.name)
                .setItems(surahs, (dialog, which) -> {
                    if (libraryView != null) libraryView.playRecitation(reciter, which + 1, surahs[which]);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void openUrl(String url) {
        try {
            Uri target=Uri.parse(url);
            if(!"https".equalsIgnoreCase(target.getScheme()) || target.getHost()==null || target.getUserInfo()!=null) throw new IllegalArgumentException("Unsupported URL");
            startActivity(new Intent(Intent.ACTION_VIEW, target));
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح الرابط على هذا الجهاز", Toast.LENGTH_SHORT).show();
        }
    }

    private enum Screen { HOME, RESULTS, FILTER, DETAIL, PREVIEW, SOURCES, FAVORITES, DISCOVER, QURAN }

    private static final class QuranReciter {
        final String name;
        final String server;
        final String surahList;
        QuranReciter(String name, String server, String surahList) {
            this.name = name == null ? "" : name;
            this.server = server == null ? "" : server;
            this.surahList = surahList == null ? "" : surahList;
        }
        boolean hasSurah(int surah) {
            if (surahList.isEmpty()) return true;
            String token = "," + surahList.replace(" ", "") + ",";
            return token.contains("," + surah + ",");
        }
    }

    private final class LibraryView extends View {

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<Hit> hits = new ArrayList<>();
        private final float tapSlop = ViewConfiguration.get(HeritageHomeActivity.this).getScaledTouchSlop();
        private float touchDownX = 0f, touchDownY = 0f;
        private boolean touchMoved = false;
        private Hit touchDownHit = null;
        private final List<BookRecord> records = new ArrayList<>();
        private final List<BookRecord> rareRecords = new ArrayList<>();
        private final Map<String, List<String>> previewCache = Collections.synchronizedMap(new HashMap<>());
        private boolean previewLoading = false;
        private int previewPageIndex = 0;
        private float previewScale = 1f;
        private float previewOffsetX = 0f, previewOffsetY = 0f;
        private float previewLastX = 0f, previewLastY = 0f;
        private float previewLastSpan = 0f;
        private boolean previewGesture = false;
        private final Map<String, Bitmap> imageCache = Collections.synchronizedMap(new HashMap<>());
        private final Set<String> imageLoading = Collections.synchronizedSet(new HashSet<>());
        private final int bg = Color.rgb(2, 12, 20);
        private final int panel = Color.rgb(5, 24, 34);
        private final int line = Color.rgb(57, 78, 82);
        private final int blue = Color.rgb(25, 151, 145);
        private final int green = Color.rgb(28, 176, 153);
        private final int gold = Color.rgb(218, 169, 82);
        private final int muted = Color.rgb(181, 181, 172);
        private final int teal = Color.rgb(20, 121, 112);
        private final int goldSoft = Color.rgb(239, 194, 108);
        private final List<QuranReciter> quranReciters = Collections.synchronizedList(new ArrayList<>());
        private boolean quranLoading = false;
        private String quranMessage = "";
        private MediaPlayer mediaPlayer = null;
        private QuranReciter playingReciter = null;
        private int playingSurah = 0;
        private String playingSurahName = "";
        private boolean quranPlaying = false;
        private final Bitmap logo;
        private float topInset = 0f, bottomInset = 0f;
        private Screen screen = Screen.HOME;
        private float homeScroll, homeContentHeight;
        private volatile boolean released;
        private String searchQuery = "ابن خلدون";
        private final SharedPreferences prefs = getSharedPreferences("heritage_library", MODE_PRIVATE);
        private String activeCategory = "";
        private boolean loading = false;
        private String networkMessage = "";
        private BookRecord selectedRecord = null;
        private int resultsPage = 0;
        private int favoritesPage = 0;
        private int filterMaxYear = 1900;
        private String filterLanguage = "";
        private String filterType = "";
        private final Map<String, List<BookRecord>> searchCache = Collections.synchronizedMap(new HashMap<>());
        private final Map<String, String> searchMessageCache = Collections.synchronizedMap(new HashMap<>());
        private long lastSearchStartedAt = 0L;

        LibraryView() {
            super(HeritageHomeActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setContentDescription("مكتبة القدس الجديدة. البحث والأقسام والنوادر. افتح القائمة للوصول إلى الفهرس المحلي وكتب PDF والمصادر.");
            setFocusable(true);
            setOnClickListener(view -> showHomeMenu());
            logo = BitmapFactory.decodeResource(getResources(), R.drawable.heritage_emblem);
            seedRealRarities();
            seedEgyptianReciters();
            // Network access starts only after an explicit reader action.
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(dp(1));
            stroke.setColor(line);
            setOnApplyWindowInsetsListener((v, insets) -> {
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                    topInset = bars.top;
                    bottomInset = bars.bottom;
                } else {
                    topInset = insets.getSystemWindowInsetTop();
                    bottomInset = insets.getSystemWindowInsetBottom();
                }
                invalidate();
                return insets;
            });
        }

        boolean goBack() {
            if (screen == Screen.HOME) return false;
            if (screen == Screen.PREVIEW) {
                previewScale = 1f; previewOffsetX = 0f; previewOffsetY = 0f; previewPageIndex = 0;
                screen = Screen.DETAIL;
            }
            else if (screen == Screen.DETAIL || screen == Screen.FILTER) screen = Screen.RESULTS;
            else screen = Screen.HOME;
            invalidate();
            return true;
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            hits.clear();
            c.drawColor(bg);
            drawAndalusianCorners(c);
            switch (screen) {
                case HOME:
                    c.save();
                    c.clipRect(0, 0, getWidth(), contentBottom()-dp(62));
                    c.translate(0, -homeScroll);
                    drawHome(c);
                    c.restore();
                    for(Hit hit:hits) hit.r.offset(0, -homeScroll);
                    break;
                case RESULTS: drawResults(c); break;
                case FILTER: drawFilter(c); break;
                case DETAIL: drawDetail(c); break;
                case PREVIEW: drawPreview(c); break;
                case SOURCES: drawSources(c); break;
                case FAVORITES: drawFavorites(c); break;
                case DISCOVER: drawDiscover(c); break;
                case QURAN: drawQuran(c); break;
            }
            if (screen != Screen.DETAIL && screen != Screen.PREVIEW && screen != Screen.FILTER) drawBottomNav(c);
        }

        private float contentTop() { return Math.max(topInset, dp(8)) + dp(8); }
        private float contentBottom() { return getHeight() - Math.max(bottomInset, dp(8)) - dp(8); }

        private void drawHome(Canvas c) {
            float w = getWidth(), y = contentTop();
            drawHeader(c, "مكتبة", "القدس الجديدة");
            y += dp(62);
            RectF search = new RectF(dp(18), y, w - dp(18), y + dp(50));
            rounded(c, search, panel, line, 14);
            text(c, "ابحث عن كتاب، مؤلف، موضوع...", search.right - dp(50), y + dp(31), dp(15), muted, Paint.Align.RIGHT, false);
            text(c, "⌕", search.left + dp(25), y + dp(33), dp(25), green, Paint.Align.CENTER, true);
            hits.add(new Hit(search, () -> showSearchDialog()));
            y += dp(64);

            RectF hero = new RectF(dp(18), y, w - dp(18), y + dp(112));
            rounded(c, hero, Color.rgb(4, 31, 34), gold, 16);
            drawArabesqueStar(c, hero.left + dp(34), hero.centerY(), dp(16));
            text(c, "القرآن المجوّد", hero.right - dp(18), hero.top + dp(34), dp(20), goldSoft, Paint.Align.RIGHT, true);
            text(c, "تلاوات كبار قرّاء مصر • اختيار السورة والاستماع", hero.right - dp(18), hero.top + dp(61), dp(11.5f), Color.WHITE, Paint.Align.RIGHT, false);
            RectF listen = new RectF(hero.right - dp(126), hero.bottom - dp(38), hero.right - dp(14), hero.bottom - dp(10));
            rounded(c, listen, Color.rgb(36, 25, 8), gold, 10);
            text(c, "استمع الآن", listen.centerX(), listen.centerY()+dp(4), dp(11), goldSoft, Paint.Align.CENTER, true);
            hits.add(new Hit(hero, () -> { screen = Screen.QURAN; fetchEgyptianRecitersAsync(); invalidate(); }));
            y += dp(128);

            sectionTitle(c, "الأقسام الرئيسية", y);
            y += dp(32);
            String[] cats = {"التاريخ", "الفقه وأصوله", "الحديث وعلومه", "القرآن وعلومه", "الجغرافيا والرحلات", "العلوم والفنون", "اللغة والأدب", "السيرة والتراجم"};
            float gap = dp(8), side = dp(18), cellW = (w - side*2 - gap*3)/4f, cellH = dp(68);
            for (int i=0;i<8;i++) {
                int row=i/4, col=3-(i%4);
                float l = side + col*(cellW+gap), t=y+row*(cellH+gap);
                RectF r = new RectF(l,t,l+cellW,t+cellH);
                rounded(c,r,Color.rgb(4,16,24),Color.rgb(58,88,50),12);
                text(c, iconFor(i), r.centerX(), r.top+dp(27), dp(21), blue, Paint.Align.CENTER, true);
                drawCategoryLabel(c,cats[i],r,cellW);
                final String categoryQuery = queryForCategory(cats[i]);
                final String categoryName = cats[i];
                hits.add(new Hit(r, () -> {
                    searchQuery = categoryQuery;
                    activeCategory = categoryName;
                    screen = Screen.RESULTS;
                    resultsPage = 0;
                    fetchUnifiedSources(categoryQuery);
                }));
            }
            y += cellH*2+gap+dp(24);
            sectionTitle(c, "نوادر المكتبة", y);
            RectF allRare=new RectF(dp(12),y-dp(20),dp(92),y+dp(12));
            rounded(c,allRare,Color.rgb(22,18,5),gold,9);
            text(c,"عرض الكل",allRare.centerX(),allRare.centerY()+dp(4),dp(10.5f),Color.WHITE,Paint.Align.CENTER,true);
            hits.add(new Hit(allRare,()->{screen=Screen.DISCOVER;invalidate();}));
            y += dp(18);
            float cardGap=dp(10), cardW=(w-dp(36)-cardGap*2)/3f;
            for(int i=0;i<3;i++){
                RectF r=new RectF(dp(18)+i*(cardW+cardGap),y,dp(18)+i*(cardW+cardGap)+cardW,y+dp(126));
                rounded(c,r,panel,line,12);
                BookRecord rare = rareRecords.isEmpty()?demoRecord():rareRecords.get(Math.min(i,rareRecords.size()-1));
                drawRecordImage(c,new RectF(r.left+dp(7),r.top+dp(7),r.right-dp(7),r.top+dp(82)),rare,i+2);
                text(c,ellipsize(rare.title,18),r.centerX(),r.bottom-dp(27),dp(11.5f),Color.WHITE,Paint.Align.CENTER,true);
                text(c,ellipsize(rare.dateLabel,18),r.centerX(),r.bottom-dp(10),dp(9.5f),muted,Paint.Align.CENTER,false);
                final BookRecord chosenRare=rare;
                hits.add(new Hit(r,()->{selectedRecord=chosenRare;screen=Screen.DETAIL;invalidate();}));
            }
            homeContentHeight=y+dp(126);
        }

        private void seedEgyptianReciters(){
            quranReciters.clear();
            quranReciters.add(new QuranReciter("الشيخ عبد الباسط عبد الصمد","",""));
            quranReciters.add(new QuranReciter("الشيخ محمد صديق المنشاوي","",""));
            quranReciters.add(new QuranReciter("الشيخ محمود خليل الحصري","",""));
            quranReciters.add(new QuranReciter("الشيخ مصطفى إسماعيل","",""));
            quranReciters.add(new QuranReciter("الشيخ محمد رفعت","",""));
        }

        private int egyptianReciterRank(String name){
            String n=name==null?"":name.replace("أ","ا").replace("إ","ا").replace("آ","ا");
            if(n.contains("عبد الباسط") && n.contains("عبد الصمد")) return 0;
            if(n.contains("محمد صديق") && n.contains("المنشاوي")) return 1;
            if(n.contains("محمود خليل") && n.contains("الحصري")) return 2;
            if(n.contains("مصطفى") && n.contains("اسماعيل")) return 3;
            if(n.contains("محمد رفعت")) return 4;
            return -1;
        }

        private void fetchEgyptianRecitersAsync(){
            if(quranLoading) return;
            quranLoading=true; quranMessage="جارٍ تحديث بيانات التلاوات...";
            new Thread(()->{
                HttpURLConnection con=null;
                try{
                    con=openConnection("https://www.mp3quran.net/api/v3/reciters?language=ar");
                    JSONObject root=new JSONObject(readBody(con));
                    JSONArray arr=root.optJSONArray("reciters");
                    QuranReciter[] selected=new QuranReciter[5];
                    if(arr!=null){
                        for(int i=0;i<arr.length();i++){
                            JSONObject rec=arr.optJSONObject(i); if(rec==null) continue;
                            String name=rec.optString("name"); int rank=egyptianReciterRank(name); if(rank<0) continue;
                            JSONArray moshaf=rec.optJSONArray("moshaf");
                            if(moshaf==null || moshaf.length()==0) continue;
                            JSONObject best=null;
                            for(int m=0;m<moshaf.length();m++){
                                JSONObject candidate=moshaf.optJSONObject(m); if(candidate==null) continue;
                                if(best==null || candidate.optInt("surah_total",0)>best.optInt("surah_total",0)) best=candidate;
                            }
                            if(best!=null && !best.optString("server").isEmpty()) selected[rank]=new QuranReciter(name,best.optString("server"),best.optString("surah_list"));
                        }
                    }
                    synchronized(quranReciters){
                        for(int i=0;i<selected.length;i++) if(selected[i]!=null) quranReciters.set(i,selected[i]);
                    }
                    quranMessage="تلاوات من المصدر الصوتي MP3Quran";
                }catch(Exception ex){
                    quranMessage="الأسماء متاحة؛ يلزم اتصال بالإنترنت لتحديث روابط التلاوة";
                }finally{
                    quranLoading=false; post(this::invalidate);
                }
            },"quran-reciter-loader").start();
        }

        private void drawQuran(Canvas c){
            float w=getWidth(), y=contentTop();
            drawPageTitle(c,"القرآن المجوّد",true); y+=dp(55);
            RectF intro=new RectF(dp(18),y,w-dp(18),y+dp(86));
            rounded(c,intro,Color.rgb(4,31,34),gold,14);
            text(c,"تلاوات خالدة من كبار شيوخ مصر",intro.right-dp(16),intro.top+dp(29),dp(16),goldSoft,Paint.Align.RIGHT,true);
            text(c,"اختر القارئ ثم السورة — تشغيل مباشر دون تنزيل الملف",intro.right-dp(16),intro.top+dp(55),dp(10.5f),Color.WHITE,Paint.Align.RIGHT,false);
            text(c,"◈",intro.left+dp(35),intro.centerY()+dp(9),dp(30),teal,Paint.Align.CENTER,true);
            y=intro.bottom+dp(14);
            if(!quranMessage.isEmpty()){
                text(c,ellipsize(quranMessage,62),w-dp(18),y,dp(9.5f),muted,Paint.Align.RIGHT,false); y+=dp(18);
            }
            List<QuranReciter> copy;
            synchronized(quranReciters){copy=new ArrayList<>(quranReciters);}
            float available=contentBottom()-dp(88)-y;
            float rowH=Math.max(dp(58),Math.min(dp(68),available/Math.max(1,copy.size())));
            for(int i=0;i<copy.size();i++){
                QuranReciter rec=copy.get(i);
                RectF r=new RectF(dp(18),y,w-dp(18),y+rowH-dp(6));
                int accent=(i%3==0)?gold:(i%3==1?teal:blue);
                rounded(c,r,panel,accent,12);
                RectF play=new RectF(r.left+dp(10),r.top+dp(9),r.left+dp(52),r.bottom-dp(9));
                rounded(c,play,Color.rgb(24,25,18),accent,20);
                boolean current=playingReciter!=null && playingReciter.name.equals(rec.name);
                text(c,current && quranPlaying?"Ⅱ":"▶",play.centerX(),play.centerY()+dp(5),dp(14),accent,Paint.Align.CENTER,true);
                text(c,rec.name,r.right-dp(14),r.top+dp(25),dp(13),Color.WHITE,Paint.Align.RIGHT,true);
                String sub=current && playingSurah>0?"يعمل الآن: سورة "+playingSurahName:(rec.server.isEmpty()?"اضغط لاختيار السورة":"اختر السورة واستمع بجودة المصدر");
                text(c,sub,r.right-dp(14),r.top+dp(47),dp(9.5f),current?goldSoft:muted,Paint.Align.RIGHT,false);
                final QuranReciter chosen=rec;
                hits.add(new Hit(r,()->{
                    if(playingReciter!=null && playingReciter.name.equals(chosen.name) && mediaPlayer!=null){toggleQuranPlayback();}
                    else if(chosen.server.isEmpty()){fetchEgyptianRecitersAsync();Toast.makeText(HeritageHomeActivity.this,"يتم تحديث رابط القارئ؛ أعد الضغط بعد لحظات",Toast.LENGTH_SHORT).show();}
                    else showSurahDialog(chosen);
                }));
                y+=rowH;
            }
            RectF refresh=new RectF(dp(18),contentBottom()-dp(126),dp(136),contentBottom()-dp(88));
            rounded(c,refresh,Color.rgb(36,25,8),gold,10);
            text(c,quranLoading?"جارٍ التحديث...":"تحديث القراء",refresh.centerX(),refresh.centerY()+dp(4),dp(10.5f),goldSoft,Paint.Align.CENTER,true);
            hits.add(new Hit(refresh,this::fetchEgyptianRecitersAsync));
        }

        private void playRecitation(QuranReciter reciter,int surah,String surahName){
            if(reciter==null || reciter.server.isEmpty()){fetchEgyptianRecitersAsync();return;}
            if(!reciter.hasSurah(surah)){Toast.makeText(HeritageHomeActivity.this,"هذه السورة غير متاحة في تسجيل هذا القارئ",Toast.LENGTH_SHORT).show();return;}
            releasePlayer();
            playingReciter=reciter; playingSurah=surah; playingSurahName=surahName; quranPlaying=false;
            String base=reciter.server.endsWith("/")?reciter.server:reciter.server+"/";
            String audioUrl=base+String.format(Locale.US,"%03d.mp3",surah);
            quranMessage="جارٍ تجهيز سورة "+surahName+" بصوت "+reciter.name+"..."; invalidate();
            try{
                MediaPlayer mp=new MediaPlayer(); mediaPlayer=mp;
                mp.setDataSource(audioUrl);
                mp.setOnPreparedListener(player->{if(mediaPlayer!=player)return;player.start();quranPlaying=true;quranMessage="تشغيل مباشر • سورة "+surahName;post(this::invalidate);});
                mp.setOnCompletionListener(player->{quranPlaying=false;post(this::invalidate);});
                mp.setOnErrorListener((player,what,extra)->{quranPlaying=false;quranMessage="تعذر تشغيل هذا التسجيل الآن";post(this::invalidate);return true;});
                mp.prepareAsync();
            }catch(Exception ex){releasePlayer();quranMessage="تعذر بدء التلاوة الآن";invalidate();}
        }

        private void toggleQuranPlayback(){
            if(mediaPlayer==null)return;
            try{
                if(mediaPlayer.isPlaying()){mediaPlayer.pause();quranPlaying=false;}
                else{mediaPlayer.start();quranPlaying=true;}
                invalidate();
            }catch(Exception ignored){}
        }

        private void releasePlayer(){
            MediaPlayer old=mediaPlayer; mediaPlayer=null; quranPlaying=false;
            if(old!=null){try{old.reset();}catch(Exception ignored){} try{old.release();}catch(Exception ignored){}}
        }

        private void drawResults(Canvas c) {
            float w=getWidth(), y=contentTop();
            drawPageTitle(c,activeCategory.isEmpty()?"نتائج البحث":activeCategory,true);
            y += dp(58);
            RectF sbox=new RectF(dp(18),y,w-dp(18),y+dp(48));
            rounded(c,sbox,panel,line,12);
            text(c,searchQuery,sbox.right-dp(18),sbox.centerY()+dp(5),dp(16),Color.WHITE,Paint.Align.RIGHT,true);
            text(c,"⌕",sbox.left+dp(25),sbox.centerY()+dp(8),dp(24),green,Paint.Align.CENTER,true);
            hits.add(new Hit(sbox,()->showSearchDialog()));
            y+=dp(64);
            RectF filterHit = new RectF(dp(12), y-dp(24), dp(86), y+dp(12));
            rounded(c,filterHit,Color.rgb(5,28,34),blue,10);
            text(c,"تصفية  ▽",filterHit.centerX(),y,dp(10.5f),Color.WHITE,Paint.Align.CENTER,true);
            hits.add(new Hit(filterHit,()->{screen=Screen.FILTER;invalidate();}));

            if (loading) {
                text(c,"جارٍ البحث الذكي في المصادر العالمية...",w/2f,y+dp(45),dp(14),blue,Paint.Align.CENTER,true);
                text(c,"نجمع ونزيل التكرار ونرتب المواد الأقدم أولًا",w/2f,y+dp(72),dp(11.5f),muted,Paint.Align.CENTER,false);
                return;
            }
            if (!networkMessage.isEmpty()) { text(c,networkMessage,w/2f,y+dp(30),dp(11),muted,Paint.Align.CENTER,false); y+=dp(48); }
            List<BookRecord> visible=filteredRecords();
            text(c,"النتائج المطابقة ("+visible.size()+")",w-dp(18),y,dp(13),green,Paint.Align.RIGHT,true);
            y+=dp(18);
            if (visible.isEmpty()) {
                text(c,"لا توجد نتائج مطابقة للفلاتر الحالية",w/2f,y+dp(45),dp(15),Color.WHITE,Paint.Align.CENTER,true);
                text(c,"يمكن تعديل التصفية أو إعادة المحاولة",w/2f,y+dp(70),dp(11.5f),muted,Paint.Align.CENTER,false);
                RectF retry=new RectF(w/2f-dp(84),y+dp(88),w/2f+dp(84),y+dp(132));
                neonButton(c,retry,"إعادة البحث");
                hits.add(new Hit(retry,()->fetchUnifiedSources(searchQuery)));
                return;
            }
            final int pageSize=3;
            int pageCount=Math.max(1,(visible.size()+pageSize-1)/pageSize);
            if(resultsPage>=pageCount) resultsPage=pageCount-1;
            if(resultsPage<0) resultsPage=0;
            int from=resultsPage*pageSize, to=Math.min(visible.size(),from+pageSize);
            for(int i=from;i<to;i++){
                BookRecord br=visible.get(i);
                RectF r=new RectF(dp(18),y,w-dp(18),y+dp(118));
                rounded(c,r,panel,line,12);
                drawRecordImage(c,new RectF(r.right-dp(94),r.top+dp(10),r.right-dp(10),r.bottom-dp(10)),br,3+(i%4));
                text(c,ellipsize(br.title,34),r.right-dp(108),r.top+dp(28),dp(14),i==from?green:Color.WHITE,Paint.Align.RIGHT,true);
                text(c,ellipsize(br.dateLabel,34),r.right-dp(108),r.top+dp(51),dp(11.5f),muted,Paint.Align.RIGHT,false);
                text(c,ellipsize(br.formatLabel,34),r.right-dp(108),r.top+dp(72),dp(11),muted,Paint.Align.RIGHT,false);
                text(c,ellipsize(br.category,24),r.right-dp(108),r.top+dp(93),dp(10),blue,Paint.Align.RIGHT,true);
                text(c,ellipsize(br.sourceName,30),r.left+dp(12),r.bottom-dp(14),dp(9.5f),green,Paint.Align.LEFT,true);
                final BookRecord chosen=br;
                hits.add(new Hit(r,()->{selectedRecord=chosen;screen=Screen.DETAIL;invalidate();}));
                y+=dp(128);
            }
            if(pageCount>1){
                RectF prev=new RectF(dp(18),contentBottom()-dp(118),dp(112),contentBottom()-dp(78));
                RectF next=new RectF(w-dp(112),contentBottom()-dp(118),w-dp(18),contentBottom()-dp(78));
                rounded(c,prev,panel,line,10); rounded(c,next,panel,line,10);
                text(c,"السابق",prev.centerX(),prev.centerY()+dp(4),dp(11),resultsPage>0?Color.WHITE:muted,Paint.Align.CENTER,true);
                text(c,"التالي",next.centerX(),next.centerY()+dp(4),dp(11),resultsPage<pageCount-1?Color.WHITE:muted,Paint.Align.CENTER,true);
                text(c,(resultsPage+1)+" / "+pageCount,w/2f,prev.centerY()+dp(4),dp(10.5f),green,Paint.Align.CENTER,true);
                hits.add(new Hit(prev,()->{if(resultsPage>0){resultsPage--;invalidate();}}));
                hits.add(new Hit(next,()->{if(resultsPage<pageCount-1){resultsPage++;invalidate();}}));
            }
        }

        private List<BookRecord> filteredRecords(){
            List<BookRecord> out=new ArrayList<>();
            for(BookRecord br:records){
                int y=extractYear(br.dateLabel);
                if(filterMaxYear<1900 && y>0 && y>filterMaxYear) continue;
                if(!filterLanguage.isEmpty() && (br.language==null || !br.language.toLowerCase(Locale.ROOT).contains(filterLanguage.toLowerCase(Locale.ROOT)))) continue;
                if(!filterType.isEmpty() && (br.formatLabel==null || !br.formatLabel.toLowerCase(Locale.ROOT).contains(filterType.toLowerCase(Locale.ROOT)))) continue;
                out.add(br);
            }
            return out;
        }

        private void drawDetail(Canvas c) {
            float w=getWidth(), y=contentTop();
            drawPageTitle(c,"",true);
            BookRecord br = selectedRecord;
            String title = br!=null ? br.title : "المقدمة لابن خلدون";
            String date = br!=null ? br.dateLabel : "القرن 8 هـ";
            String fmt = br!=null ? br.formatLabel : "نسخة خطية";
            String sourceUrl = br!=null ? br.url : "https://www.qdl.qa/";
            BookRecord favoriteRecord = br!=null ? br : demoRecord();
            boolean currentFav = isFavorite(favoriteRecord);
            RectF favHit = new RectF(w-dp(62), contentTop(), w-dp(8), contentTop()+dp(56));
            text(c,currentFav?"♥":"♡",favHit.centerX(),contentTop()+dp(36),dp(27),currentFav?green:Color.WHITE,Paint.Align.CENTER,true);
            hits.add(new Hit(favHit,()->{
                boolean now=toggleFavorite(favoriteRecord);
                invalidate();
                Toast.makeText(HeritageHomeActivity.this, now?"تمت الإضافة إلى المفضلة":"تمت الإزالة من المفضلة", Toast.LENGTH_SHORT).show();
            }));
            y+=dp(42);
            RectF art=new RectF(dp(18),y,w-dp(18),y+dp(215));
            drawRecordImage(c,art,favoriteRecord,5);
            y+=dp(238);
            text(c,ellipsize(title,38),w/2,y,dp(22),Color.WHITE,Paint.Align.CENTER,true);
            y+=dp(28);
            text(c,fmt,w/2,y,dp(15),blue,Paint.Align.CENTER,true);
            y+=dp(24);
            text(c,date,w/2,y,dp(13),muted,Paint.Align.CENTER,false);
            y+=dp(22);
            RectF meta=new RectF(dp(18),y,w-dp(18),y+dp(175));
            rounded(c,meta,panel,line,14);
            String[][] rows={{"القسم", br!=null?br.category:"التاريخ"},{"المجال", br!=null?br.subject:"التراث والتاريخ"},{"اللغة",br!=null?br.language:"العربية"},{"التاريخ",date},{"نوع المادة",fmt},{"المصدر",br!=null?br.sourceName:"مكتبة قطر الرقمية"}};
            float ry=meta.top+dp(27);
            for(String[] row:rows){
                text(c,row[0],meta.right-dp(18),ry,dp(11.5f),muted,Paint.Align.RIGHT,false);
                text(c,ellipsize(row[1],28),meta.left+dp(18),ry,dp(11.5f),Color.WHITE,Paint.Align.LEFT,false);
                ry+=dp(24);
            }
            y=meta.bottom+dp(18);
            RectF read=new RectF(dp(18),y,w-dp(18),y+dp(48));
            neonButton(c,read,"عرض / قراءة من المصدر");
            hits.add(new Hit(read,()->openUrl(sourceUrl)));
            y+=dp(60);
            if(br!=null && !br.manifestUrl.isEmpty()){
                RectF preview=new RectF(dp(18),y,w-dp(18),y+dp(44));
                rounded(c,preview,Color.rgb(4,22,28),green,12);
                text(c,"معاينة صفحات المخطوطة",preview.centerX(),preview.centerY()+dp(5),dp(12.5f),green,Paint.Align.CENTER,true);
                hits.add(new Hit(preview,()->{
                    previewPageIndex=0; previewScale=1f; previewOffsetX=0f; previewOffsetY=0f;
                    screen=Screen.PREVIEW;fetchPreviewPages(br);invalidate();
                }));
                y+=dp(56);
            }
            if(y+dp(44)<=contentBottom()-dp(6)){
                RectF source=new RectF(dp(18),y,w/2-dp(5),y+dp(44));
                RectF share=new RectF(w/2+dp(5),y,w-dp(18),y+dp(44));
                rounded(c,source,panel,line,12); rounded(c,share,panel,line,12);
                text(c,"المصدر الأصلي",source.centerX(),source.centerY()+dp(5),dp(12),Color.WHITE,Paint.Align.CENTER,true);
                text(c,"مشاركة",share.centerX(),share.centerY()+dp(5),dp(12),Color.WHITE,Paint.Align.CENTER,true);
                hits.add(new Hit(source,()->openUrl(sourceUrl)));
                hits.add(new Hit(share,()->{
                    Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,"مكتبة القدس الجديدة — "+title+"\n"+sourceUrl); startActivity(Intent.createChooser(i,"مشاركة"));
                }));
            }
        }

        private void drawPreview(Canvas c) {
            float w=getWidth(), y=contentTop();
            drawPageTitle(c,"معاينة الصفحات",true);
            y+=dp(60);
            BookRecord br=selectedRecord;
            if(br==null){text(c,"لا توجد مخطوطة محددة",w/2f,y+dp(40),dp(15),Color.WHITE,Paint.Align.CENTER,true);return;}
            text(c,ellipsize(br.title,42),w-dp(18),y,dp(14.5f),Color.WHITE,Paint.Align.RIGHT,true);
            y+=dp(24);
            text(c,"المصدر: "+br.sourceName,w-dp(18),y,dp(10.5f),green,Paint.Align.RIGHT,false);
            y+=dp(16);
            List<String> pages=previewCache.get(br.manifestUrl);
            if(previewLoading && (pages==null || pages.isEmpty())){
                text(c,"جارٍ تحميل صفحات المخطوطة من المصدر الأصلي...",w/2f,y+dp(70),dp(13),blue,Paint.Align.CENTER,true);
                return;
            }
            if(pages==null || pages.isEmpty()){
                text(c,"لم تتوفر معاينات صفحات لهذه المادة",w/2f,y+dp(70),dp(13),muted,Paint.Align.CENTER,true);
                return;
            }
            if(previewPageIndex<0) previewPageIndex=0;
            if(previewPageIndex>=pages.size()) previewPageIndex=pages.size()-1;

            float controlsH=dp(80);
            RectF viewport=new RectF(dp(14),y+dp(12),w-dp(14),contentBottom()-controlsH-dp(10));
            rounded(c,viewport,Color.rgb(3,12,19),line,12);
            c.save();
            Path clip=new Path(); clip.addRoundRect(viewport,dp(10),dp(10),Path.Direction.CW); c.clipPath(clip);
            String u=pages.get(previewPageIndex);
            Bitmap bm=imageCache.get(u);
            if(bm!=null && !bm.isRecycled()){
                float availableW=viewport.width()-dp(12), availableH=viewport.height()-dp(12);
                float base=Math.min(availableW/bm.getWidth(),availableH/bm.getHeight());
                float dw=bm.getWidth()*base*previewScale, dh=bm.getHeight()*base*previewScale;
                float cx=viewport.centerX()+previewOffsetX, cy=viewport.centerY()+previewOffsetY;
                RectF dest=new RectF(cx-dw/2f,cy-dh/2f,cx+dw/2f,cy+dh/2f);
                c.drawBitmap(bm,null,dest,p);
            }else{
                drawManuscript(c,new RectF(viewport.left+dp(10),viewport.top+dp(10),viewport.right-dp(10),viewport.bottom-dp(10)),previewPageIndex%6);
                loadImageAsync(u);
            }
            c.restore();

            text(c,"صفحة "+(previewPageIndex+1)+" من "+pages.size(),w/2f,viewport.bottom+dp(23),dp(11.5f),Color.WHITE,Paint.Align.CENTER,true);
            RectF prev=new RectF(dp(18),viewport.bottom+dp(31),dp(110),viewport.bottom+dp(69));
            RectF reset=new RectF(w/2f-dp(52),viewport.bottom+dp(31),w/2f+dp(52),viewport.bottom+dp(69));
            RectF next=new RectF(w-dp(110),viewport.bottom+dp(31),w-dp(18),viewport.bottom+dp(69));
            rounded(c,prev,panel,line,10); rounded(c,reset,panel,line,10); rounded(c,next,panel,line,10);
            text(c,"السابق",prev.centerX(),prev.centerY()+dp(4),dp(11),previewPageIndex>0?Color.WHITE:muted,Paint.Align.CENTER,true);
            text(c,"100%",reset.centerX(),reset.centerY()+dp(4),dp(11),green,Paint.Align.CENTER,true);
            text(c,"التالي",next.centerX(),next.centerY()+dp(4),dp(11),previewPageIndex<pages.size()-1?Color.WHITE:muted,Paint.Align.CENTER,true);
            hits.add(new Hit(prev,()->{if(previewPageIndex>0){previewPageIndex--;resetPreviewTransform();invalidate();}}));
            hits.add(new Hit(next,()->{if(previewPageIndex<pages.size()-1){previewPageIndex++;resetPreviewTransform();invalidate();}}));
            hits.add(new Hit(reset,()->{resetPreviewTransform();invalidate();}));
            text(c,"قرّب بإصبعين واسحب الصفحة عند التكبير",w/2f,contentBottom()-dp(2),dp(9.5f),muted,Paint.Align.CENTER,false);
        }

        private void resetPreviewTransform(){
            previewScale=1f; previewOffsetX=0f; previewOffsetY=0f;
        }

        private void drawSources(Canvas c) {
            float w=getWidth(), y=contentTop();
            drawPageTitle(c,"المصادر",false);
            y+=dp(58);
            text(c,"المصادر الرسمية التي تعتمد عليها مكتبة القدس الجديدة",w/2f,y,dp(10.5f),muted,Paint.Align.CENTER,false);
            y+=dp(18);
            String[] names={"مكتبة قطر الرقمية","المكتبة الرقمية العالمية","Digital Bodleian — Oxford","أرشيف الإنترنت","مكتبة الكونغرس"};
            String[] desc={"مخطوطات ووثائق عربية وإسلامية","مجموعات تاريخية نادرة عالمية","مخطوطات مصورة وIIIF من أكسفورد","كتب قديمة ومخطوطات رقمية","مجموعات وكتب تاريخية نادرة"};
            String[] count={"QDL","WDL","IIIF","IA","LOC"};
            String[] urls={"https://www.qdl.qa/","https://www.loc.gov/collections/world-digital-library/","https://digital.bodleian.ox.ac.uk/","https://archive.org/","https://www.loc.gov/"};
            int[] accents={green,blue,gold,Color.rgb(255,96,64),Color.rgb(190,130,255)};
            float navTop=contentBottom()-dp(68);
            float available=Math.max(dp(350),navTop-y-dp(8));
            float cardH=Math.min(dp(86),available/5f-dp(6));
            cardH=Math.max(dp(68),cardH);
            for(int i=0;i<5;i++){
                RectF r=new RectF(dp(18),y,w-dp(18),y+cardH);
                rounded(c,r,panel,accents[i],14);
                RectF icon=new RectF(r.left+dp(10),r.top+dp(9),r.left+dp(58),r.bottom-dp(9));
                rounded(c,icon,Color.rgb(7,24,34),accents[i],12);
                text(c,i==0?"☾":i==1?"◎":i==2?"✦":i==3?"▣":"⌂",icon.centerX(),icon.centerY()+dp(7),dp(22),accents[i],Paint.Align.CENTER,true);
                RectF open=new RectF(r.right-dp(82),r.bottom-dp(31),r.right-dp(10),r.bottom-dp(7));
                rounded(c,open,Color.rgb(8,28,24),accents[i],9);
                text(c,"فتح",open.centerX(),open.centerY()+dp(4),dp(10.5f),Color.WHITE,Paint.Align.CENTER,true);
                text(c,names[i],r.right-dp(12),r.top+dp(23),dp(14),Color.WHITE,Paint.Align.RIGHT,true);
                text(c,ellipsize(desc[i],38),r.right-dp(12),r.top+dp(43),dp(9.5f),muted,Paint.Align.RIGHT,false);
                text(c,count[i],r.left+dp(68),r.bottom-dp(10),dp(9.5f),accents[i],Paint.Align.LEFT,true);
                final String u=urls[i];
                hits.add(new Hit(r,()->openUrl(u)));
                y+=cardH+dp(6);
            }
        }


        private void drawDiscover(Canvas c){
            float w=getWidth(), y=contentTop();
            drawPageTitle(c,"مرصد التراث",false);
            y+=dp(58);
            text(c,"استكشف المعرفة كشبكة مترابطة — لا كقائمة كتب",w/2f,y,dp(11),muted,Paint.Align.CENTER,false);
            y+=dp(24);

            float cx=w/2f, cy=y+dp(128);
            float orbit=Math.min(dp(116),w*0.28f);
            stroke.setStrokeWidth(dp(1)); stroke.setColor(Color.argb(115,0,168,255));
            c.drawCircle(cx,cy,orbit,stroke);
            c.drawCircle(cx,cy,orbit*0.62f,stroke);
            p.setShadowLayer(dp(18),0,0,Color.argb(140,124,255,0));
            p.setColor(Color.rgb(5,33,20)); c.drawCircle(cx,cy,dp(37),p); p.clearShadowLayer();
            stroke.setColor(green); c.drawCircle(cx,cy,dp(37),stroke);
            text(c,"القدس",cx,cy+dp(3),dp(12),gold,Paint.Align.CENTER,true);
            text(c,"تراث",cx,cy+dp(18),dp(9),Color.WHITE,Paint.Align.CENTER,false);

            String[] nodeNames={"الحديث","التاريخ","الطب","الجغرافيا","الأدب","الفقه"};
            String[] nodeQueries={"حديث مخطوطات عربية","تاريخ إسلامي مخطوطات","طب عربي مخطوطات ابن سينا","جغرافيا رحلات الإدريسي","أدب عربي قديم","فقه أصول الفقه مخطوطات"};
            for(int i=0;i<nodeNames.length;i++){
                double a=(-Math.PI/2)+(Math.PI*2*i/nodeNames.length);
                float nx=cx+(float)Math.cos(a)*orbit, ny=cy+(float)Math.sin(a)*orbit;
                stroke.setColor(Color.argb(95,124,255,0)); c.drawLine(cx,cy,nx,ny,stroke);
                RectF nr=new RectF(nx-dp(31),ny-dp(20),nx+dp(31),ny+dp(20));
                rounded(c,nr,Color.rgb(4,18,27),i%2==0?blue:green,20);
                text(c,nodeNames[i],nx,ny+dp(4),dp(10),Color.WHITE,Paint.Align.CENTER,true);
                final String q=nodeQueries[i], cat=nodeNames[i];
                hits.add(new Hit(nr,()->{searchQuery=q;activeCategory=cat;screen=Screen.RESULTS;resultsPage=0;fetchUnifiedSources(q);}));
            }
            y=cy+orbit+dp(34);
            sectionTitle(c,"آلة الزمن",y); y+=dp(22);
            text(c,"اقفز مباشرة إلى قرنٍ من التراث",w-dp(18),y,dp(10),muted,Paint.Align.RIGHT,false); y+=dp(18);
            String[] eras={"8هـ","10هـ","12هـ","14هـ","16م","18م","19م"};
            String[] eraQueries={"manuscript 8th century hijri Arabic","manuscript 10th century hijri Arabic","manuscript 12th century hijri Arabic","manuscript 14th century hijri Arabic","Arabic manuscript 16th century","Arabic book 18th century","Arabic book 19th century"};
            float side=dp(18), gap=dp(7), bw=(w-side*2-gap*3)/4f, bh=dp(38);
            for(int i=0;i<eras.length;i++){
                int row=i/4,col=3-(i%4); float l=side+col*(bw+gap),t=y+row*(bh+gap);
                RectF r=new RectF(l,t,l+bw,t+bh); rounded(c,r,panel,i%2==0?line:Color.rgb(55,88,52),10);
                text(c,eras[i],r.centerX(),r.centerY()+dp(4),dp(10.5f),i%2==0?Color.WHITE:green,Paint.Align.CENTER,true);
                final String q=eraQueries[i]; final String label=eras[i];
                hits.add(new Hit(r,()->{searchQuery=q;activeCategory="القرن "+label;screen=Screen.RESULTS;resultsPage=0;fetchUnifiedSources(q);}));
            }
            y+=2*(bh+gap)+dp(22);
            sectionTitle(c,"مسارات ذكية",y); y+=dp(20);
            String[] paths={"ابن خلدون ← التاريخ ← المغرب والأندلس","ابن سينا ← الطب ← المخطوطات العلمية","الإدريسي ← الجغرافيا ← الرحلات والخرائط"};
            String[] pq={"ابن خلدون المغرب الأندلس تاريخ","ابن سينا طب مخطوطات عربية","الإدريسي جغرافيا رحلات خرائط"};
            for(int i=0;i<3;i++){
                RectF r=new RectF(dp(18),y,w-dp(18),y+dp(42)); rounded(c,r,Color.rgb(4,16,24),i==1?green:line,11);
                text(c,paths[i],r.right-dp(12),r.centerY()+dp(4),dp(10.5f),Color.WHITE,Paint.Align.RIGHT,i==1);
                text(c,"›",r.left+dp(18),r.centerY()+dp(7),dp(22),i==1?green:blue,Paint.Align.CENTER,true);
                final String q=pq[i]; hits.add(new Hit(r,()->{searchQuery=q;activeCategory="مسار ذكي";screen=Screen.RESULTS;resultsPage=0;fetchUnifiedSources(q);}));
                y+=dp(49);
                if(y>contentBottom()-dp(130)) break;
            }
            RectF surprise=new RectF(dp(18),contentBottom()-dp(124),w-dp(18),contentBottom()-dp(76));
            neonButton(c,surprise,"✦ اكتشاف مخطوطة عشوائية");
            hits.add(new Hit(surprise,()->{
                if(!rareRecords.isEmpty()){
                    int idx=(int)(System.currentTimeMillis()%rareRecords.size());
                    selectedRecord=rareRecords.get(idx);screen=Screen.DETAIL;invalidate();
                } else { searchQuery="rare Arabic manuscript";activeCategory="اكتشاف";screen=Screen.RESULTS;resultsPage=0;fetchUnifiedSources(searchQuery);}
            }));
        }

        private void drawFavorites(Canvas c){
            float w=getWidth(); drawPageTitle(c,"المفضلة",false);
            float y=contentTop()+dp(68); List<BookRecord> saved=loadFavorites();
            text(c,"الكتب المحفوظة ("+saved.size()+")",w-dp(18),y,dp(13),green,Paint.Align.RIGHT,true); y+=dp(22);
            if(saved.isEmpty()){
                if(logo!=null){RectF r=new RectF(w/2f-dp(62),y+dp(12),w/2f+dp(62),y+dp(144));c.drawBitmap(logo,null,r,p);}
                y+=dp(182); text(c,"لا توجد عناصر محفوظة بعد",w/2f,y,dp(17),Color.WHITE,Paint.Align.CENTER,true);
                y+=dp(30); text(c,"يمكن حفظ أي كتاب أو مخطوطة من زر القلب",w/2f,y,dp(12),muted,Paint.Align.CENTER,false); return;
            }
            final int pageSize=3; int pageCount=Math.max(1,(saved.size()+pageSize-1)/pageSize);
            if(favoritesPage>=pageCount) favoritesPage=pageCount-1; if(favoritesPage<0) favoritesPage=0;
            int from=favoritesPage*pageSize,to=Math.min(saved.size(),from+pageSize);
            for(int i=from;i<to;i++){
                BookRecord br=saved.get(i); RectF r=new RectF(dp(18),y,w-dp(18),y+dp(118)); rounded(c,r,panel,line,14);
                drawRecordImage(c,new RectF(r.right-dp(94),r.top+dp(10),r.right-dp(10),r.bottom-dp(10)),br,5+(i%3));
                text(c,ellipsize(br.title,34),r.right-dp(108),r.top+dp(30),dp(14),Color.WHITE,Paint.Align.RIGHT,true);
                text(c,ellipsize(br.dateLabel,30),r.right-dp(108),r.top+dp(55),dp(11.5f),muted,Paint.Align.RIGHT,false);
                text(c,ellipsize(br.category,24),r.right-dp(108),r.top+dp(78),dp(10.5f),blue,Paint.Align.RIGHT,true);
                text(c,ellipsize(br.sourceName,30),r.left+dp(12),r.bottom-dp(14),dp(9.5f),green,Paint.Align.LEFT,true);
                final BookRecord chosen=br; hits.add(new Hit(r,()->{selectedRecord=chosen;screen=Screen.DETAIL;invalidate();})); y+=dp(128);
            }
            if(pageCount>1){
                RectF prev=new RectF(dp(18),contentBottom()-dp(118),dp(112),contentBottom()-dp(78));
                RectF next=new RectF(w-dp(112),contentBottom()-dp(118),w-dp(18),contentBottom()-dp(78));
                rounded(c,prev,panel,line,10); rounded(c,next,panel,line,10);
                text(c,"السابق",prev.centerX(),prev.centerY()+dp(4),dp(11),favoritesPage>0?Color.WHITE:muted,Paint.Align.CENTER,true);
                text(c,"التالي",next.centerX(),next.centerY()+dp(4),dp(11),favoritesPage<pageCount-1?Color.WHITE:muted,Paint.Align.CENTER,true);
                text(c,(favoritesPage+1)+" / "+pageCount,w/2f,prev.centerY()+dp(4),dp(10.5f),green,Paint.Align.CENTER,true);
                hits.add(new Hit(prev,()->{if(favoritesPage>0){favoritesPage--;invalidate();}}));
                hits.add(new Hit(next,()->{if(favoritesPage<pageCount-1){favoritesPage++;invalidate();}}));
            }
        }

        private void drawFilter(Canvas c){
            float w=getWidth(), y=contentTop(); drawPageTitle(c,"تصفية النتائج",true); y += dp(66);
            sectionTitle(c,"الفترة الزمنية",y); y+=dp(22);
            String[] labels={"الكل حتى 1900","قبل 1800","قبل 1600","قبل 1400","قبل 1200","قبل 1000"};
            int[] years={1900,1800,1600,1400,1200,1000};
            float gap=dp(8), side=dp(18), bw=(w-side*2-gap*2)/3f, bh=dp(42);
            for(int i=0;i<labels.length;i++){
                int row=i/3,col=2-(i%3); float l=side+col*(bw+gap),t=y+row*(bh+gap); RectF r=new RectF(l,t,l+bw,t+bh);
                boolean sel=filterMaxYear==years[i]; rounded(c,r,sel?Color.rgb(13,41,18):panel,sel?green:line,10);
                text(c,labels[i],r.centerX(),r.centerY()+dp(5),dp(10),sel?green:Color.WHITE,Paint.Align.CENTER,sel);
                final int yy=years[i]; hits.add(new Hit(r,()->{filterMaxYear=yy;resultsPage=0;invalidate();}));
            }
            y += 2*(bh+gap)+dp(22); sectionTitle(c,"اللغة",y); y+=dp(24);
            String[] langs={"الكل","العربية","الفارسية","التركية"}; String[] lvals={"","العرب","pers","turk"};
            for(int i=0;i<4;i++){ float l=w-dp(18)-(i+1)*dp(78)-i*dp(7); RectF r=new RectF(l,y,l+dp(78),y+dp(42)); boolean sel=filterLanguage.equals(lvals[i]); rounded(c,r,sel?Color.rgb(13,41,18):panel,sel?green:line,10); text(c,langs[i],r.centerX(),r.centerY()+dp(5),dp(10.5f),sel?green:Color.WHITE,Paint.Align.CENTER,sel); final String lv=lvals[i]; hits.add(new Hit(r,()->{filterLanguage=lv;resultsPage=0;invalidate();})); }
            y+=dp(70); sectionTitle(c,"نوع المادة",y); y+=dp(24);
            String[] types={"الكل","مخطوطة","كتاب","خريطة"}; String[] tvals={"","مخط","كتاب","خريط"};
            for(int i=0;i<4;i++){ float t=y+i*dp(42); RectF r=new RectF(dp(18),t,w-dp(18),t+dp(34)); boolean sel=filterType.equals(tvals[i]); text(c,sel?"☑":"□",w-dp(28),t+dp(22),dp(19),sel?green:muted,Paint.Align.CENTER,true); text(c,types[i],w-dp(52),t+dp(21),dp(12.5f),Color.WHITE,Paint.Align.RIGHT,false); final String tv=tvals[i]; hits.add(new Hit(r,()->{filterType=tv;resultsPage=0;invalidate();})); }
            RectF apply=new RectF(dp(18),contentBottom()-dp(124),w-dp(18),contentBottom()-dp(74)); neonButton(c,apply,"تطبيق التصفية");
            hits.add(new Hit(apply,()->{screen=Screen.RESULTS;resultsPage=0;invalidate();}));
        }

        private void fetchUnifiedSources(String query){
            resultsPage=0;
            final String cacheKey=(query==null?"":query.trim().toLowerCase(Locale.ROOT));
            List<BookRecord> cached=searchCache.get(cacheKey);
            if(cached!=null && !cached.isEmpty()){
                records.clear(); records.addAll(new ArrayList<>(cached));
                loading=false; networkMessage=searchMessageCache.getOrDefault(cacheKey,"نتائج محفوظة — تحديث في الخلفية");
                invalidate();
            } else {
                loading=true; networkMessage="جارٍ إحضار أسرع النتائج..."; records.clear(); invalidate();
            }
            lastSearchStartedAt=System.currentTimeMillis();
            final long generation=lastSearchStartedAt;
            final List<BookRecord> merged=Collections.synchronizedList(new ArrayList<>());
            final String[] errors={"","","",""};
            final AtomicInteger completed=new AtomicInteger(0);
            final ExecutorService pool=Executors.newFixedThreadPool(4);

            Runnable[] jobs=new Runnable[]{
                    ()->{try{mergeProgressive(merged,fetchLocRecords(query),cacheKey,generation);}catch(Exception ex){errors[0]="LOC";}finally{completed.incrementAndGet();}},
                    ()->{try{mergeProgressive(merged,fetchQdlRecords(query),cacheKey,generation);}catch(Exception ex){errors[1]="QDL";}finally{completed.incrementAndGet();}},
                    ()->{try{mergeProgressive(merged,fetchInternetArchiveRecords(query),cacheKey,generation);}catch(Exception ex){errors[2]="IA";}finally{completed.incrementAndGet();}},
                    ()->{try{mergeProgressive(merged,fetchBodleianCuratedRecords(query),cacheKey,generation);}catch(Exception ex){errors[3]="BOD";}finally{completed.incrementAndGet();}}
            };
            for(Runnable job:jobs) pool.submit(job);
            pool.shutdown();

            new Thread(()->{
                try{ pool.awaitTermination(9, TimeUnit.SECONDS); }catch(InterruptedException e){Thread.currentThread().interrupt();}
                pool.shutdownNow();
                List<BookRecord> ready=normalizeRecords(merged);
                int failed=0; for(String error:errors) if(!error.isEmpty()) failed++;
                String msg;
                if(ready.isEmpty()) msg=failed>=3?"تعذر الوصول إلى أغلب المصادر الآن — جرّب مجددًا":"لم نجد مواد قديمة مطابقة في المصادر التي استجابت";
                else if(completed.get()<4 || failed>0) msg="تم عرض النتائج السريعة؛ بعض المصادر البطيئة تم تجاوزها";
                else msg=sourceCountMessage(ready);
                searchCache.put(cacheKey,new ArrayList<>(ready)); searchMessageCache.put(cacheKey,msg);
                if(generation==lastSearchStartedAt) post(()->{records.clear();records.addAll(ready);loading=false;networkMessage=msg;invalidate();});
            }).start();
        }

        private void mergeProgressive(List<BookRecord> merged,List<BookRecord> incoming,String cacheKey,long generation){
            if(incoming==null || incoming.isEmpty()) return;
            synchronized(merged){merged.addAll(incoming);}
            List<BookRecord> snapshot=normalizeRecords(merged);
            searchCache.put(cacheKey,new ArrayList<>(snapshot));
            if(generation==lastSearchStartedAt) post(()->{
                records.clear(); records.addAll(snapshot); loading=false;
                networkMessage="ظهرت النتائج الأولى — بقية المصادر تُستكمل في الخلفية";
                invalidate();
            });
        }

        private List<BookRecord> normalizeRecords(List<BookRecord> input){
            List<BookRecord> copy;
            synchronized(input){copy=new ArrayList<>(input);}
            List<BookRecord> unique=new ArrayList<>();
            for(BookRecord candidate:copy){
                boolean exists=false;
                for(BookRecord current:unique){
                    if(current.url.equals(candidate.url) || (current.title.equalsIgnoreCase(candidate.title) && current.sourceName.equals(candidate.sourceName))){exists=true;break;}
                }
                if(!exists) unique.add(candidate);
            }
            Collections.sort(unique,(a,b)->{
                int ay=extractYear(a.dateLabel),by=extractYear(b.dateLabel);
                if(ay<0&&by>=0)return 1; if(by<0&&ay>=0)return -1;
                if(ay>=0&&by>=0)return Integer.compare(ay,by);
                return a.title.compareToIgnoreCase(b.title);
            });
            if(unique.size()>40) return new ArrayList<>(unique.subList(0,40));
            return unique;
        }

        private String sourceCountMessage(List<BookRecord> list){
            int qdl=0,loc=0,ia=0,bod=0;
            for(BookRecord r:list){if("QDL".equals(r.sourceCode))qdl++;else if("LOC".equals(r.sourceCode))loc++;else if("IA".equals(r.sourceCode))ia++;else if("BOD".equals(r.sourceCode))bod++;}
            return "نتائج موحّدة: قطر "+qdl+" • العالمية "+loc+" • أكسفورد "+bod+" • الأرشيف "+ia;
        }

        private List<BookRecord> fetchLocRecords(String query) throws Exception{
            List<BookRecord> found=new ArrayList<>();
            String q=URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String endpoint="https://www.loc.gov/collections/world-digital-library/?q="+q+"&fo=json&c=50";
            HttpURLConnection con=openConnection(endpoint);
            String body=readBody(con);
            JSONArray arr=new JSONObject(body).optJSONArray("results");
            if(arr!=null){
                for(int i=0;i<arr.length() && found.size()<30;i++){
                    JSONObject o=arr.optJSONObject(i); if(o==null) continue;
                    String title=o.optString("title","بدون عنوان");
                    String date=o.optString("date","");
                    int year=extractYear(date);
                    if(year>1900) continue;
                    String url=o.optString("url",""); if(url.isEmpty()) continue;
                    String format=joinJson(o.optJSONArray("original_format"),"مادة تراثية");
                    String language=joinJson(o.optJSONArray("language"),"غير محددة");
                    String subject=joinJson(o.optJSONArray("subject"),"تراث");
                    String category=classifyCategory(title, subject, format);
                    String imageUrl=firstJsonString(o.optJSONArray("image_url"));
                    found.add(new BookRecord(title,date.isEmpty()?"تاريخ غير محدد":date,format,language,subject,category,url,"المكتبة الرقمية العالمية / مكتبة الكونغرس","LOC",imageUrl));
                }
            }
            return found;
        }

        private List<BookRecord> fetchQdlRecords(String query) throws Exception{
            List<BookRecord> found=new ArrayList<>();
            String encoded=URLEncoder.encode(query, StandardCharsets.UTF_8.name()).replace("+","%20");
            String[] endpoints={
                    "https://www.qdl.qa/en/search/site/"+encoded+"?f%5B0%5D=document_source%3Aarchive_source",
                    "https://www.qdl.qa/en/search/site?search_api_views_fulltext="+encoded+"&f%5B0%5D=document_source%3Aarchive_source"
            };
            String html="";
            Exception last=null;
            for(String endpoint:endpoints){
                try{
                    HttpURLConnection con=openConnection(endpoint);
                    html=readBody(con);
                    if(html.contains("/en/archive/") || html.contains("/archive/")) break;
                }catch(Exception ex){last=ex;}
            }
            if(html.isEmpty()){ if(last!=null) throw last; return found; }

            Pattern linkPattern=Pattern.compile("<a[^>]+href=\"([^\"]*(?:/en)?/archive/[^\"]+)\"[^>]*>(.*?)</a>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
            Matcher m=linkPattern.matcher(html);
            while(m.find() && found.size()<30){
                String url=m.group(1);
                if(url.startsWith("/")) url="https://www.qdl.qa"+url;
                if(!url.startsWith("http")) continue;
                String title=cleanHtml(m.group(2));
                if(title.length()<3) continue;
                int contextStart=Math.max(0,m.start()-500), contextEnd=Math.min(html.length(),m.end()+1200);
                String context=cleanHtml(html.substring(contextStart,contextEnd));
                String date=extractQdlDate(context);
                int year=extractYear(date);
                if(year>1900) continue;
                String lower=context.toLowerCase();
                String format=(lower.contains("manuscript") || lower.contains("مخطوط"))?"مخطوطة":"كتاب / مادة تراثية";
                String language=(lower.contains("arabic") || context.contains("العربية"))?"العربية":"غير محددة";
                String subject="تراث عربي وإسلامي";
                String category=classifyCategory(title, context, format);
                String rawContext=html.substring(contextStart,contextEnd);
                String imageUrl=extractImageUrl(rawContext,"https://www.qdl.qa");
                found.add(new BookRecord(title,date.isEmpty()?"تاريخ غير محدد":date,format,language,subject,category,url,"مكتبة قطر الرقمية","QDL",imageUrl));
            }
            return found;
        }

        private List<BookRecord> fetchInternetArchiveRecords(String query) throws Exception{
            List<BookRecord> found=new ArrayList<>();
            String search="("+query+") AND mediatype:texts AND year:[0 TO 1900]";
            String q=URLEncoder.encode(search, StandardCharsets.UTF_8.name());
            String endpoint="https://archive.org/advancedsearch.php?q="+q
                    +"&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator&fl%5B%5D=date&fl%5B%5D=year"
                    +"&fl%5B%5D=language&fl%5B%5D=subject&fl%5B%5D=description&rows=50&page=1&output=json";
            HttpURLConnection con=openConnection(endpoint);
            String body=readBody(con);
            JSONObject response=new JSONObject(body).optJSONObject("response");
            JSONArray docs=response==null?null:response.optJSONArray("docs");
            if(docs==null) return found;
            for(int i=0;i<docs.length() && found.size()<30;i++){
                JSONObject o=docs.optJSONObject(i); if(o==null) continue;
                String identifier=o.optString("identifier",""); if(identifier.isEmpty()) continue;
                String title=jsonValueToText(o.opt("title"),"بدون عنوان");
                String date=jsonValueToText(o.opt("date"),jsonValueToText(o.opt("year"),""));
                int year=extractYear(date);
                if(year>1900) continue;
                String language=jsonValueToText(o.opt("language"),"غير محددة");
                String subject=jsonValueToText(o.opt("subject"),"تراث");
                String description=jsonValueToText(o.opt("description"),"");
                String format="كتاب مطبوع قديم / نص رقمي";
                String category=classifyCategory(title, subject+" "+description, format);
                String url="https://archive.org/details/"+identifier;
                String imageUrl="https://archive.org/services/img/"+identifier;
                found.add(new BookRecord(title,date.isEmpty()?"تاريخ غير محدد":date,format,language,subject,category,url,"أرشيف الإنترنت","IA",imageUrl));
            }
            return found;
        }

        private void seedRealRarities(){
            rareRecords.clear();
            rareRecords.add(new BookRecord("كتاب البلهان","1399","مخطوطة عربية","العربية","فلك وتنجيم وصور تراثية","العلوم والفنون","https://digital.bodleian.ox.ac.uk/inquire/p/9f5d7f6d-843d-49c2-aee3-18ea29ccf273","Digital Bodleian — Oxford","BOD","","https://iiif.bodleian.ox.ac.uk/iiif/manifest/9f5d7f6d-843d-49c2-aee3-18ea29ccf273.json"));
            rareRecords.add(new BookRecord("De materia medica — نسخة عربية","1240","مخطوطة مصورة","العربية","طب ونبات وصيدلة","العلوم والفنون","https://digital.bodleian.ox.ac.uk/inquire/p/a13ff6ad-5858-45f1-9b06-56e034d68500","Digital Bodleian — Oxford","BOD","","https://iiif.bodleian.ox.ac.uk/iiif/manifest/a13ff6ad-5858-45f1-9b06-56e034d68500.json"));
            rareRecords.add(new BookRecord("Zij i jadid-i Sultani","القرن 15–16","مخطوطة عربية","العربية","فلك وجداول تقويمية","العلوم والفنون","https://digital.bodleian.ox.ac.uk/inquire/p/4c03cc08-80b8-45bc-94f9-a02be81a4784","Digital Bodleian — Oxford","BOD","","https://iiif.bodleian.ox.ac.uk/iiif/manifest/4c03cc08-80b8-45bc-94f9-a02be81a4784.json"));
            rareRecords.add(new BookRecord("كليلة ودمنة — MS. Pococke 400","القرن 14","مخطوطة عربية مصورة","العربية","أدب وحِكم وحكايات","اللغة والأدب","https://digital.bodleian.ox.ac.uk/objects/8face4cc-d7dc-4ec6-8315-64b8c171dd76/","Digital Bodleian — Oxford","BOD","","https://iiif.bodleian.ox.ac.uk/iiif/manifest/8face4cc-d7dc-4ec6-8315-64b8c171dd76.json"));
            rareRecords.add(new BookRecord("نزهة المشتاق للإدريسي — MS. Pococke 375","1553","مخطوطة جغرافية عربية","العربية","جغرافيا وخرائط العالم","الجغرافيا والرحلات","https://digital.bodleian.ox.ac.uk/objects/ced0d8bd-1019-4af2-9086-e411115f1507/","Digital Bodleian — Oxford","BOD","","https://iiif.bodleian.ox.ac.uk/iiif/manifest/ced0d8bd-1019-4af2-9086-e411115f1507.json"));
            rareRecords.add(new BookRecord("كتاب غرائب الفنون وملح العيون","1190–1210","مخطوطة كوزموغرافية عربية","العربية","جغرافيا وفلك وخرائط","الجغرافيا والرحلات","https://digital.bodleian.ox.ac.uk/objects/748a9d50-5a3a-440e-ab9d-567dd68b6abb/","Digital Bodleian — Oxford","BOD","","https://iiif.bodleian.ox.ac.uk/iiif/manifest/748a9d50-5a3a-440e-ab9d-567dd68b6abb.json"));
            rareRecords.add(new BookRecord("نسخة جغرافية عربية — MS. Greaves 42","القرن 16","مخطوطة خرائط عربية","العربية","جغرافيا وخرائط","الجغرافيا والرحلات","https://digital.bodleian.ox.ac.uk/objects/c99d2caf-7abc-4593-97a7-02bf17213f35/","Digital Bodleian — Oxford","BOD","","https://iiif.bodleian.ox.ac.uk/iiif/manifest/c99d2caf-7abc-4593-97a7-02bf17213f35.json"));
        }

        private void loadBodleianRaritiesAsync(){
            new Thread(()->{
                List<BookRecord> updated=new ArrayList<>();
                for(BookRecord br:new ArrayList<>(rareRecords)){
                    try{updated.add(enrichFromIiifManifest(br));}catch(Exception ex){updated.add(br);}
                }
                post(()->{rareRecords.clear();rareRecords.addAll(updated);invalidate();});
            }).start();
        }

        private List<BookRecord> fetchBodleianCuratedRecords(String query) throws Exception{
            List<BookRecord> out=new ArrayList<>();
            String q=query==null?"":query.toLowerCase(Locale.ROOT);
            for(BookRecord base:new ArrayList<>(rareRecords)){
                String hay=(base.title+" "+base.subject+" "+base.category+" عربي arabic manuscript مخطوطة").toLowerCase(Locale.ROOT);
                boolean broad=q.length()<3 || containsAny(q,"مخطوط","عربي","تراث","علوم","فلك","طب","manuscript","arabic","science","astronomy","medicine");
                if(broad || hay.contains(q) || wordsOverlap(q,hay)){
                    try{out.add(enrichFromIiifManifest(base));}catch(Exception ex){out.add(base);}
                }
            }
            return out;
        }

        private boolean wordsOverlap(String q,String hay){
            for(String token:q.split("\\s+")) if(token.length()>2 && hay.contains(token)) return true;
            return false;
        }

        private BookRecord enrichFromIiifManifest(BookRecord base) throws Exception{
            if(base.manifestUrl.isEmpty()) return base;
            HttpURLConnection con=openConnection(base.manifestUrl);
            JSONObject manifest=new JSONObject(readBody(con));
            String title=base.title;
            Object label=manifest.opt("label");
            if(label instanceof String && !((String)label).trim().isEmpty()) title=(String)label;
            List<String> pages=extractIiifImages(manifest);
            if(!pages.isEmpty()) previewCache.put(base.manifestUrl,pages);
            String image=pages.isEmpty()?base.imageUrl:pages.get(0);
            return new BookRecord(title,base.dateLabel,base.formatLabel,base.language,base.subject,base.category,base.url,base.sourceName,base.sourceCode,image,base.manifestUrl);
        }

        private List<String> extractIiifImages(JSONObject manifest){
            List<String> urls=new ArrayList<>();
            JSONArray sequences=manifest.optJSONArray("sequences");
            if(sequences!=null && sequences.length()>0){
                JSONArray canvases=sequences.optJSONObject(0)==null?null:sequences.optJSONObject(0).optJSONArray("canvases");
                if(canvases!=null){
                    for(int i=0;i<canvases.length() && urls.size()<80;i++){
                        JSONObject canvas=canvases.optJSONObject(i); if(canvas==null) continue;
                        JSONArray images=canvas.optJSONArray("images"); if(images==null||images.length()==0) continue;
                        JSONObject resource=images.optJSONObject(0)==null?null:images.optJSONObject(0).optJSONObject("resource");
                        if(resource==null) continue;
                        JSONObject service=resource.optJSONObject("service");
                        String u="";
                        if(service!=null){String id=service.optString("@id",service.optString("id","")); if(!id.isEmpty()) u=id+"/full/600,/0/default.jpg";}
                        if(u.isEmpty()) u=resource.optString("@id",resource.optString("id",""));
                        if(u.startsWith("http")) urls.add(u);
                    }
                }
            }
            return urls;
        }

        private void fetchPreviewPages(BookRecord br){
            if(br==null || br.manifestUrl.isEmpty()) return;
            previewPageIndex=0; resetPreviewTransform();
            List<String> cached=previewCache.get(br.manifestUrl);
            if(cached!=null && !cached.isEmpty()){previewLoading=false;invalidate();return;}
            previewLoading=true;
            new Thread(()->{
                try{
                    HttpURLConnection con=openConnection(br.manifestUrl);
                    JSONObject manifest=new JSONObject(readBody(con));
                    List<String> pages=extractIiifImages(manifest);
                    previewCache.put(br.manifestUrl,pages);
                }catch(Exception ignored){}
                post(()->{previewLoading=false;invalidate();});
            }).start();
        }

        private String jsonValueToText(Object value,String fallback){
            if(value==null || value==JSONObject.NULL) return fallback;
            if(value instanceof JSONArray){
                JSONArray arr=(JSONArray)value; StringBuilder out=new StringBuilder();
                for(int i=0;i<arr.length() && i<3;i++){ if(i>0) out.append("، "); out.append(arr.optString(i)); }
                return out.length()==0?fallback:out.toString();
            }
            String out=String.valueOf(value).trim();
            return out.isEmpty()?fallback:out;
        }

        private String queryForCategory(String category){
            if("التاريخ".equals(category)) return "تاريخ history chronicles";
            if("الفقه وأصوله".equals(category)) return "فقه أصول الشريعة fiqh jurisprudence";
            if("الحديث وعلومه".equals(category)) return "حديث السنة hadith";
            if("القرآن وعلومه".equals(category)) return "قرآن تفسير quran tafsir";
            if("الجغرافيا والرحلات".equals(category)) return "رحلات جغرافيا travel geography";
            if("العلوم والفنون".equals(category)) return "طب فلك رياضيات medicine astronomy mathematics";
            if("اللغة والأدب".equals(category)) return "لغة أدب شعر grammar literature poetry";
            if("السيرة والتراجم".equals(category)) return "سيرة تراجم biographies genealogy";
            return category;
        }

        private String classifyCategory(String title,String subject,String format){
            String text=(title+" "+subject+" "+format).toLowerCase(Locale.ROOT);
            if(containsAny(text,"حديث","hadith","sunnah","سنن","مسند","صحيح البخاري","صحيح مسلم")) return "الحديث وعلومه";
            if(containsAny(text,"قرآن","quran","koran","tafsir","تفسير","قراءات","مصحف")) return "القرآن وعلومه";
            if(containsAny(text,"فقه","fiqh","jurisprudence","شريعة","sharia","أصول الفقه","فتاوى")) return "الفقه وأصوله";
            if(containsAny(text,"سيرة","biograph","تراجم","أنساب","genealog","طبقات","رجال")) return "السيرة والتراجم";
            if(containsAny(text,"جغرافيا","geograph","رحلة","رحلات","travel","voyage","map","خريطة","atlas")) return "الجغرافيا والرحلات";
            if(containsAny(text,"طب","medicine","medical","فلك","astronom","رياضيات","mathemat","science","صيدلة","pharmac")) return "العلوم والفنون";
            if(containsAny(text,"لغة","نحو","صرف","بلاغة","أدب","شعر","poetry","literature","grammar","dictionary","قاموس")) return "اللغة والأدب";
            if(containsAny(text,"تاريخ","history","chronicle","annals","دولة","سلطنة","مملكة","عثمان","egypt","مصر","andalus","الأندلس")) return "التاريخ";
            return "تراث عام";
        }

        private boolean containsAny(String text,String... needles){
            for(String needle:needles) if(text.contains(needle.toLowerCase(Locale.ROOT))) return true;
            return false;
        }

        private HttpURLConnection openConnection(String endpoint) throws Exception{
            if(released || Thread.currentThread().isInterrupted()) throw new java.io.IOException("Request cancelled");
            URL target=new URL(endpoint);
            if(!"https".equalsIgnoreCase(target.getProtocol()) || target.getUserInfo()!=null) throw new java.io.IOException("HTTPS required");
            HttpURLConnection con=(HttpURLConnection)target.openConnection();
            con.setConnectTimeout(5500); con.setReadTimeout(7500);
            con.setInstanceFollowRedirects(true);
            con.setRequestProperty("User-Agent","AlQuds-New-Library/1.3 (Android; heritage discovery client)");
            con.setRequestProperty("Accept-Language","ar,en;q=0.8");
            int code=con.getResponseCode();
            if(code<200 || code>=400) throw new Exception("HTTP "+code);
            return con;
        }

        private String readBody(HttpURLConnection con) throws Exception{
            try(InputStream input=con.getInputStream(); java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()){
                byte[] buffer=new byte[8192]; int count;
                while((count=input.read(buffer))!=-1){
                    if(released || Thread.currentThread().isInterrupted() || output.size()+count>2*1024*1024) throw new java.io.IOException("Response limit");
                    output.write(buffer,0,count);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }finally{con.disconnect();}
        }

        private String cleanHtml(String value){
            if(value==null) return "";
            String without=value.replaceAll("(?is)<script.*?</script>"," ").replaceAll("(?is)<style.*?</style>"," ").replaceAll("(?is)<[^>]+>"," ");
            String decoded=Build.VERSION.SDK_INT>=24?Html.fromHtml(without,Html.FROM_HTML_MODE_LEGACY).toString():Html.fromHtml(without).toString();
            return decoded.replaceAll("\\s+"," ").trim();
        }

        private String extractQdlDate(String context){
            if(context==null) return "";
            Matcher labeled=Pattern.compile("(?i)(?:Date\\(s\\)|Date|التاريخ)\\s*[:：]?\\s*([^|]{0,80}?)(?=(?:Ref|Reference|Extent|images|صفحة|$))").matcher(context);
            if(labeled.find()){
                String d=labeled.group(1).trim();
                if(!d.isEmpty()) return d;
            }
            Matcher year=Pattern.compile("(?<!\\d)(1[0-8]\\d{2}|1900)(?!\\d)").matcher(context);
            if(year.find()) return year.group(1);
            return "";
        }

        private int extractYear(String value){
            if(value==null) return -1;
            for(int i=0;i+3<value.length();i++){
                char a=value.charAt(i),b=value.charAt(i+1),d=value.charAt(i+2),e=value.charAt(i+3);
                if(Character.isDigit(a)&&Character.isDigit(b)&&Character.isDigit(d)&&Character.isDigit(e)){
                    try{return Integer.parseInt(value.substring(i,i+4));}catch(Exception ignored){}
                }
            }
            return -1;
        }

        private String joinJson(JSONArray arr,String fallback){
            if(arr==null || arr.length()==0) return fallback;
            StringBuilder out=new StringBuilder();
            for(int i=0;i<arr.length() && i<2;i++){ if(i>0) out.append("، "); out.append(arr.optString(i)); }
            return out.length()==0?fallback:out.toString();
        }

        private String ellipsize(String value,int max){
            if(value==null) return "";
            return value.length()>max?value.substring(0,Math.max(1,max-1))+"…":value;
        }

        final class BookRecord{
            final String title,dateLabel,formatLabel,language,subject,category,url,sourceName,sourceCode,imageUrl,manifestUrl;
            BookRecord(String title,String dateLabel,String formatLabel,String language,String subject,String category,String url,String sourceName,String sourceCode,String imageUrl){this(title,dateLabel,formatLabel,language,subject,category,url,sourceName,sourceCode,imageUrl,"");}
            BookRecord(String title,String dateLabel,String formatLabel,String language,String subject,String category,String url,String sourceName,String sourceCode,String imageUrl,String manifestUrl){
                this.title=title;this.dateLabel=dateLabel;this.formatLabel=formatLabel;this.language=language;this.subject=subject;this.category=category;this.url=url;this.sourceName=sourceName;this.sourceCode=sourceCode;this.imageUrl=imageUrl==null?"":imageUrl;this.manifestUrl=manifestUrl==null?"":manifestUrl;
            }
            JSONObject toJson(){
                JSONObject o=new JSONObject();
                try{o.put("title",title);o.put("date",dateLabel);o.put("format",formatLabel);o.put("language",language);o.put("subject",subject);o.put("category",category);o.put("url",url);o.put("source",sourceName);o.put("code",sourceCode);o.put("image",imageUrl);o.put("manifest",manifestUrl);}catch(Exception ignored){}
                return o;
            }
        }

        private BookRecord demoRecord(){
            return new BookRecord("المقدمة لابن خلدون","القرن 8 هـ","نسخة خطية","العربية","التراث والتاريخ","التاريخ","https://www.qdl.qa/","مكتبة قطر الرقمية","QDL","");
        }

        private String favoriteId(BookRecord br){ return Integer.toHexString((br.sourceCode+"|"+br.url+"|"+br.title).hashCode()); }
        private List<BookRecord> loadFavorites(){
            List<BookRecord> out=new ArrayList<>();
            String raw=prefs.getString("favorites_json","[]");
            try{
                JSONArray arr=new JSONArray(raw);
                for(int i=0;i<arr.length();i++){
                    JSONObject o=arr.optJSONObject(i); if(o==null) continue;
                    out.add(new BookRecord(o.optString("title"),o.optString("date"),o.optString("format"),o.optString("language"),o.optString("subject"),o.optString("category"),o.optString("url"),o.optString("source"),o.optString("code"),o.optString("image"),o.optString("manifest")));
                }
            }catch(Exception ignored){}
            return out;
        }
        private boolean isFavorite(BookRecord br){
            String id=favoriteId(br);
            for(BookRecord x:loadFavorites()) if(favoriteId(x).equals(id)) return true;
            return false;
        }
        private boolean toggleFavorite(BookRecord br){
            List<BookRecord> items=loadFavorites(); String id=favoriteId(br); boolean removed=false;
            for(int i=items.size()-1;i>=0;i--) if(favoriteId(items.get(i)).equals(id)){items.remove(i);removed=true;}
            if(!removed) items.add(0,br);
            JSONArray arr=new JSONArray(); for(BookRecord x:items) arr.put(x.toJson());
            prefs.edit().putString("favorites_json",arr.toString()).apply();
            return !removed;
        }

        private String firstJsonString(JSONArray arr){
            if(arr==null) return "";
            for(int i=0;i<arr.length();i++){String v=arr.optString(i,"");if(v.startsWith("//")) v="https:"+v;if(v.startsWith("http")) return v;}
            return "";
        }
        private String extractImageUrl(String html,String base){
            if(html==null) return "";
            Matcher m=Pattern.compile("<img[^>]+src=[\\\"']([^\\\"']+)[\\\"']",Pattern.CASE_INSENSITIVE).matcher(html);
            while(m.find()){
                String u=m.group(1).replace("&amp;","&");
                if(u.startsWith("//")) u="https:"+u; else if(u.startsWith("/")) u=base+u;
                if(u.startsWith("http")) return u;
            }
            return "";
        }
        private void drawRecordImage(Canvas c,RectF r,BookRecord br,int variant){
            if(br==null || br.imageUrl==null || br.imageUrl.isEmpty()){drawManuscript(c,r,variant);return;}
            Bitmap bm=imageCache.get(br.imageUrl);
            if(bm!=null && !bm.isRecycled()){
                p.setColor(Color.WHITE); c.drawRoundRect(r,dp(7),dp(7),p);
                c.save(); Path clip=new Path(); clip.addRoundRect(r,dp(7),dp(7),Path.Direction.CW); c.clipPath(clip); c.drawBitmap(bm,null,r,p); c.restore();
                stroke.setColor(line);c.drawRoundRect(r,dp(7),dp(7),stroke);
            }else{
                drawManuscript(c,r,variant); loadImageAsync(br.imageUrl);
            }
        }
        private void loadImageAsync(String imageUrl){
            if(released || imageUrl==null || imageUrl.isEmpty() || imageCache.size()>=12 || imageCache.containsKey(imageUrl) || !imageLoading.add(imageUrl)) return;
            new Thread(()->{
                HttpURLConnection con=null;
                try{
                    con=openConnection(imageUrl);
                    byte[] bytes;
                    try(InputStream input=con.getInputStream(); java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()){
                        byte[] buffer=new byte[8192]; int count;
                        while((count=input.read(buffer))!=-1){
                            if(released || output.size()+count>4*1024*1024) throw new java.io.IOException("Image limit");
                            output.write(buffer,0,count);
                        }
                        bytes=output.toByteArray();
                    }
                    BitmapFactory.Options bounds=new BitmapFactory.Options(); bounds.inJustDecodeBounds=true;
                    BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
                    if(bounds.outWidth<=0 || bounds.outHeight<=0) return;
                    BitmapFactory.Options options=new BitmapFactory.Options(); options.inSampleSize=1;
                    while(bounds.outWidth/options.inSampleSize>1024 || bounds.outHeight/options.inSampleSize>1024) options.inSampleSize*=2;
                    Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                    if(bitmap!=null){synchronized(imageCache){if(!released && imageCache.size()<12) imageCache.put(imageUrl,bitmap);else bitmap.recycle();}}
                }catch(Exception ignored){}finally{if(con!=null) con.disconnect();imageLoading.remove(imageUrl);post(this::invalidate);}
            },"heritage-thumbnail").start();
        }

        private void dispose(){
            released=true; releasePlayer();
            synchronized(imageCache){for(Bitmap bitmap:imageCache.values()) if(!bitmap.isRecycled()) bitmap.recycle();imageCache.clear();}
            if(logo!=null && !logo.isRecycled()) logo.recycle();
        }

        private void drawHeader(Canvas c,String ar,String en){
            float y=contentTop()+dp(24);
            RectF menu=new RectF(dp(8),contentTop(),dp(52),contentTop()+dp(44));
            rounded(c,menu,Color.rgb(28,20,6),gold,12);
            text(c,"☰",menu.centerX(),menu.centerY()+dp(7),dp(22),Color.WHITE,Paint.Align.CENTER,true);
            hits.add(new Hit(menu,()->showHomeMenu()));
            text(c,"مكتبة القدس الجديدة",getWidth()-dp(18),y,dp(20),goldSoft,Paint.Align.RIGHT,true);
            text(c,"الكتب والمخطوطات القديمة والنادرة",getWidth()-dp(18),y+dp(24),dp(10.5f),Color.rgb(194,166,112),Paint.Align.RIGHT,false);
        }

        private void drawPageTitle(Canvas c,String title,boolean back){
            float y=contentTop()+dp(26);
            if(back){
                text(c,"‹",dp(26),y+dp(4),dp(34),Color.WHITE,Paint.Align.CENTER,false);
                hits.add(new Hit(new RectF(0,contentTop(),dp(58),contentTop()+dp(58)),()->goBack()));
            }
            if(!title.isEmpty()) text(c,title,getWidth()/2f,y,dp(21),Color.WHITE,Paint.Align.CENTER,true);
        }

        private void drawBottomNav(Canvas c){
            float y=contentBottom()-dp(62), w=getWidth();
            p.setColor(Color.rgb(2,11,18)); c.drawRect(0,y,w,contentBottom()+dp(8),p);
            stroke.setColor(line); c.drawLine(0,y,w,y,stroke);
            String[] labs={"الرئيسية","المكتبة","المفضلة","المصادر","المرصد"};
            String[] icons={"⌂","▤","♡","▥","✦"};
            Screen[] screens={Screen.HOME,Screen.RESULTS,Screen.FAVORITES,Screen.SOURCES,Screen.DISCOVER};
            for(int i=0;i<5;i++){
                float cx=w-(i+0.5f)*w/5f;
                boolean active=(screen==screens[i]) || (screen==Screen.DETAIL&&i==1);
                int col=active?goldSoft:Color.rgb(185,194,200);
                text(c,icons[i],cx,y+dp(25),dp(20),col,Paint.Align.CENTER,true);
                text(c,labs[i],cx,y+dp(47),dp(9.5f),col,Paint.Align.CENTER,false);
                final Screen s=screens[i];
                hits.add(new Hit(new RectF(cx-w/10f,y,cx+w/10f,contentBottom()),()->{if(s==Screen.RESULTS){showLibraryMenu();}else{screen=s;invalidate();}}));
            }
        }

        private void drawAndalusianCorners(Canvas c){
            stroke.setStrokeWidth(dp(1)); stroke.setColor(Color.argb(120,0,168,255));
            float s=dp(42);
            for(int corner=0;corner<4;corner++){
                float ox=(corner%2==0)?dp(8):getWidth()-dp(8);
                float oy=(corner<2)?Math.max(topInset,0)+dp(6):getHeight()-Math.max(bottomInset,0)-dp(6);
                Path path=new Path();
                float sx=(corner%2==0)?1:-1, sy=(corner<2)?1:-1;
                path.moveTo(ox,oy+sy*s); path.lineTo(ox+sx*s*.35f,oy+sy*s*.7f); path.lineTo(ox+sx*s*.7f,oy+sy*s*.35f); path.lineTo(ox+sx*s,oy);
                c.drawPath(path,stroke);
                for(int i=1;i<=3;i++) c.drawCircle(ox+sx*s*i/4f,oy+sy*s*(4-i)/4f,dp(2.2f),stroke);
            }
            drawArabesqueStar(c,getWidth()/2f,Math.max(topInset,0)+dp(10),dp(7));
        }

        private void drawArabesqueStar(Canvas c,float cx,float cy,float radius){
            Path star=new Path();
            for(int i=0;i<16;i++){
                double a=-Math.PI/2+i*Math.PI/8.0;
                float rr=(i%2==0)?radius:radius*0.43f;
                float x=cx+(float)Math.cos(a)*rr, y=cy+(float)Math.sin(a)*rr;
                if(i==0)star.moveTo(x,y);else star.lineTo(x,y);
            }
            star.close(); stroke.setColor(Color.argb(170,184,137,61)); stroke.setStrokeWidth(dp(1)); c.drawPath(star,stroke);
        }

        private void drawManuscript(Canvas c,RectF r,int variant){
            p.setColor(Color.rgb(123+variant*4,84+variant*2,42)); c.drawRoundRect(r,dp(7),dp(7),p);
            p.setColor(Color.rgb(202,165,98)); c.drawRoundRect(new RectF(r.left+dp(5),r.top+dp(5),r.right-dp(5),r.bottom-dp(5)),dp(5),dp(5),p);
            stroke.setColor(Color.rgb(117,78,34)); stroke.setStrokeWidth(dp(1));
            float mid=r.centerX(); c.drawLine(mid,r.top+dp(5),mid,r.bottom-dp(5),stroke);
            for(int i=0;i<9;i++){
                float yy=r.top+dp(16)+i*(r.height()-dp(30))/9f;
                c.drawLine(r.left+dp(13),yy,mid-dp(7),yy,stroke);
                c.drawLine(mid+dp(7),yy,r.right-dp(13),yy,stroke);
            }
            stroke.setColor(Color.rgb(49,84,86));
            RectF frame=new RectF(r.left+dp(9),r.top+dp(9),r.right-dp(9),r.bottom-dp(9)); c.drawRoundRect(frame,dp(3),dp(3),stroke);
            text(c,"غلاف توضيحي",r.centerX(),r.bottom-dp(6),dp(8),Color.rgb(45,31,15),Paint.Align.CENTER,true);
        }

        private void sectionTitle(Canvas c,String s,float y){text(c,s,getWidth()-dp(18),y,dp(17),goldSoft,Paint.Align.RIGHT,true);}
        private String iconFor(int i){String[] a={"▦","♙","◎","▥","✎","◉","▤","♧"};return a[i%a.length];}

        private void rounded(Canvas c,RectF r,int fill,int border,float radiusDp){
            p.setStyle(Paint.Style.FILL); p.setColor(fill); c.drawRoundRect(r,dp(radiusDp),dp(radiusDp),p);
            stroke.setColor(border); stroke.setStrokeWidth(dp(1)); c.drawRoundRect(r,dp(radiusDp),dp(radiusDp),stroke);
        }
        private void neonButton(Canvas c,RectF r,String label){
            int accent=green, fill=Color.rgb(5,33,20);
            if(label.contains("قراءة") || label.contains("عرض")){accent=blue;fill=Color.rgb(4,27,43);}
            else if(label.contains("استكشف")){accent=gold;fill=Color.rgb(38,27,8);}
            p.setShadowLayer(dp(10),0,0,Color.argb(135,Color.red(accent),Color.green(accent),Color.blue(accent)));
            rounded(c,r,fill,accent,12); p.clearShadowLayer();
            text(c,label,r.centerX(),r.centerY()+dp(5),dp(13),Color.WHITE,Paint.Align.CENTER,true);
        }
        private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align,boolean bold){
            p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size); p.setTextAlign(align); p.setTypeface(bold?android.graphics.Typeface.DEFAULT_BOLD:android.graphics.Typeface.DEFAULT); c.drawText(s,x,y,p);
        }
        private float fitTextSizePx(String value,float maxWidth,float preferredDp,float minDp,boolean bold){
            p.setTypeface(bold?android.graphics.Typeface.DEFAULT_BOLD:android.graphics.Typeface.DEFAULT);
            float size=dp(preferredDp), min=dp(minDp);
            p.setTextSize(size);
            while(size>min && p.measureText(value)>maxWidth){ size-=dp(0.5f); p.setTextSize(size); }
            return Math.max(size,min);
        }
        private void drawCategoryLabel(Canvas c,String label,RectF r,float cellW){
            String first=label, second="";
            int split=label.indexOf(" و");
            if(label.length()>10 && split>0){ first=label.substring(0,split); second=label.substring(split+1); }
            float max=cellW-dp(8);
            if(second.isEmpty()){
                float size=fitTextSizePx(first,max,10.5f,7.5f,false);
                text(c,first,r.centerX(),r.bottom-dp(10),size,Color.WHITE,Paint.Align.CENTER,false);
            }else{
                float size1=fitTextSizePx(first,max,9.5f,7.5f,false);
                float size2=fitTextSizePx(second,max,9.0f,7.0f,false);
                text(c,first,r.centerX(),r.bottom-dp(21),size1,Color.WHITE,Paint.Align.CENTER,false);
                text(c,second,r.centerX(),r.bottom-dp(8),size2,Color.WHITE,Paint.Align.CENTER,false);
            }
        }
        private float dp(float v){return v*getResources().getDisplayMetrics().density;}

        @Override
        public boolean onTouchEvent(MotionEvent e){
            if(screen==Screen.PREVIEW){
                int action=e.getActionMasked();
                if(action==MotionEvent.ACTION_DOWN){
                    previewLastX=e.getX(); previewLastY=e.getY(); previewGesture=false;
                }else if(action==MotionEvent.ACTION_POINTER_DOWN && e.getPointerCount()>=2){
                    previewLastSpan=pointerSpan(e); previewGesture=true;
                }else if(action==MotionEvent.ACTION_MOVE){
                    if(e.getPointerCount()>=2){
                        float span=pointerSpan(e);
                        if(previewLastSpan>0f){
                            float factor=span/previewLastSpan;
                            previewScale=Math.max(1f,Math.min(5f,previewScale*factor));
                            if(previewScale<=1.01f){previewOffsetX=0f;previewOffsetY=0f;}
                            invalidate();
                        }
                        previewLastSpan=span; previewGesture=true;
                    }else if(previewScale>1.01f){
                        float dx=e.getX()-previewLastX, dy=e.getY()-previewLastY;
                        if(Math.abs(dx)>1f || Math.abs(dy)>1f){
                            previewOffsetX+=dx; previewOffsetY+=dy; previewGesture=true; invalidate();
                        }
                        previewLastX=e.getX(); previewLastY=e.getY();
                    }
                }else if(action==MotionEvent.ACTION_POINTER_UP){
                    previewLastSpan=0f;
                }else if(action==MotionEvent.ACTION_UP){
                    if(!previewGesture){
                        float x=e.getX(),y=e.getY();
                        for(int i=hits.size()-1;i>=0;i--){if(hits.get(i).r.contains(x,y)){hits.get(i).action.run();return true;}}
                    }
                    previewLastSpan=0f; previewGesture=false;
                }
                return true;
            }
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                touchDownX=e.getX(); touchDownY=e.getY(); touchMoved=false; touchDownHit=findHit(touchDownX,touchDownY);
                return true;
            }
            if(action==MotionEvent.ACTION_MOVE){
                float dx=e.getX()-touchDownX, dy=e.getY()-touchDownY;
                if(!touchMoved && (dx*dx+dy*dy) > tapSlop*tapSlop) touchMoved=true;
                if(screen==Screen.HOME && touchMoved){
                    homeScroll=Math.max(0f,Math.min(Math.max(0f,homeContentHeight-(contentBottom()-dp(62))),homeScroll-dy));
                    touchDownX=e.getX(); touchDownY=e.getY();
                    invalidate();
                }
                return true;
            }
            if(action==MotionEvent.ACTION_CANCEL){
                touchMoved=false; touchDownHit=null; return true;
            }
            if(action==MotionEvent.ACTION_UP){
                float x=e.getX(),y=e.getY();
                Hit upHit=findHit(x,y);
                if(!touchMoved && touchDownHit!=null && upHit!=null && sameHit(touchDownHit,upHit)){
                    touchDownHit.action.run();
                }
                touchMoved=false; touchDownHit=null;
                return true;
            }
            return true;
        }

        private boolean sameHit(Hit a, Hit b){
            if(a==null || b==null) return false;
            final float eps=dp(1f);
            return Math.abs(a.r.left-b.r.left)<=eps && Math.abs(a.r.top-b.r.top)<=eps
                    && Math.abs(a.r.right-b.r.right)<=eps && Math.abs(a.r.bottom-b.r.bottom)<=eps;
        }

        private Hit findHit(float x,float y){
            for(int i=hits.size()-1;i>=0;i--){
                Hit h=hits.get(i);
                if(screen==Screen.HOME && i<hits.size()-5 && y>=contentBottom()-dp(62)) continue;
                if(h.r.contains(x,y)) return h;
            }
            return null;
        }

        private float pointerSpan(MotionEvent e){
            if(e.getPointerCount()<2) return 0f;
            float dx=e.getX(0)-e.getX(1), dy=e.getY(0)-e.getY(1);
            return (float)Math.sqrt(dx*dx+dy*dy);
        }

        final class Hit { final RectF r; final Runnable action; Hit(RectF r,Runnable action){this.r=new RectF(r);this.action=action;} }
    }
}
