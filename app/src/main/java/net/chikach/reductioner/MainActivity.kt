package net.chikach.reductioner

import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.CircularProgressDrawable
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.accessibility.AccessibilityManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawer = findViewById<DrawerLayout>(R.id.drawerLayout)
        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)

        val sp = PreferenceManager.getDefaultSharedPreferences(this)

        navigationView.menu.findItem(R.id.pref1).icon = if (sp.getBoolean("pref1", false)) {
            resources.getDrawable(R.drawable.check, null)
        } else {
            resources.getDrawable(R.drawable.no_check, null)
        }

        startButton.setOnClickListener { it ->
            if (numerator_text.text.isEmpty() || denomirator_text.text.isEmpty()) {
                toast("分子と分母の両方に入力してください。")
                return@setOnClickListener
            }
            val start = System.currentTimeMillis()
            val shouldShow = !sp.getBoolean("pref1", false)
            val pd = ProgressDialog(this)
            pd.setProgressStyle(if (shouldShow) ProgressDialog.STYLE_HORIZONTAL else ProgressDialog.STYLE_SPINNER)
            pd.setMessage("計算中です。しばらくお待ち下さい。")
            pd.setTitle("計算中")
            pd.setCancelable(false)
            try {
                if (shouldShow) pd.max = numerator_text.text.toString().toInt() + denomirator_text.text.toString().toInt()
            } catch (e: NumberFormatException) {
                it.snackbar("数字が大きすぎます。「プログレスバーを表示しない」をオンにするか、もっと約分して入力してください。")
                return@setOnClickListener
            }
            pd.show()
            val handler = Handler()
            val thread = Thread {
                try {
                    val numeratorDivisor = mutableListOf<Long>()
                    val denomiratorDivisor = mutableListOf<Long>()

                    val numerator = numerator_text.text.toString().toLong()
                    val denomirator = denomirator_text.text.toString().toLong()

                    if (numerator == 0L || denomirator == 0L) {
                        handler.post {
                            toast("0で除算することはできません。")
                        }
                        pd.dismiss()
                        return@Thread
                    }

                    for (n in 1..numerator) {
                        if (numerator % n == 0L) {
                            numeratorDivisor.add(numerator / n)
                        }
                        if (shouldShow) pd.progress++
                    }
                    for (n in 1..denomirator) {
                        if (denomirator % n == 0L) {
                            denomiratorDivisor.add(denomirator / n)
                        }
                        if (shouldShow) pd.progress++
                    }
                    val divisor = numeratorDivisor.asSequence().filter { denomiratorDivisor.contains(it) }.max()

                    numerator_text.setText((numerator / divisor!!).toString())
                    denomirator_text.setText((denomirator / divisor).toString())

                    handler.post {
                        toast("かかった時間(ミリ秒):${System.currentTimeMillis() - start}")
                    }

                    pd.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                    handler.post {
                        toast("計算に失敗しました。")
                    }
                }
            }
            thread.start()
        }
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when (p0.itemId) {
            R.id.pref1 -> {
                val sp = PreferenceManager.getDefaultSharedPreferences(this)
                if (!sp.getBoolean("pref1", false)) {
                    sp.edit().putBoolean("pref1", true).apply()
                    p0.icon = resources.getDrawable(R.drawable.check, null)
                } else {
                    sp.edit().putBoolean("pref1", false).apply()
                    p0.icon = resources.getDrawable(R.drawable.no_check, null)
                }
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun showSnackBar(view: View, text: String, length: Int) {
        Snackbar.make(view, text, length).apply {
            //                        if (shouldForceAnimate) {
            try {
                val accManagerField = BaseTransientBottomBar::class.java.getDeclaredField("mAccessibilityManager")
                accManagerField.isAccessible = true
                val accManager = accManagerField.get(this)
                AccessibilityManager::class.java.getDeclaredField("mIsEnabled").apply {
                    isAccessible = true
                    setBoolean(accManager, false)
                }
                accManagerField.set(this, accManager)
            } catch (e: Exception) {
                Log.d("SnackBar", "Reflection error: $e")
            }
//                        }
        }.show()
    }
}
