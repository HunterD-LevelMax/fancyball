package com.fancyball.fancyball

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private var progressAnimator: ValueAnimator? = null
    private var logoAnimator: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(R.color.transparent)
        window.navigationBarColor = getColor(R.color.transparent)
        setContentView(R.layout.activity_splash)

        val progress = findViewById<ProgressBar>(R.id.splashProgress)
        val logo = findViewById<android.view.View>(R.id.logoContainer)
        val subtitle = findViewById<TextView>(R.id.splashSubtitle)

        logoAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, android.view.View.SCALE_X, 0.94f, 1.04f, 1f),
                ObjectAnimator.ofFloat(logo, android.view.View.SCALE_Y, 0.94f, 1.04f, 1f),
                ObjectAnimator.ofFloat(logo, android.view.View.ROTATION, -4f, 4f, 0f)
            )
            duration = 1200L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        progressAnimator = ValueAnimator.ofInt(0, 1000).apply {
            duration = SPLASH_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Int
                progress.progress = value
                subtitle.text = getString(R.string.loading_percent_format, value / 10)
            }
            start()
        }

        progress.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_DURATION_MS)
    }

    override fun onDestroy() {
        progressAnimator?.cancel()
        logoAnimator?.cancel()
        progressAnimator = null
        logoAnimator = null
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DURATION_MS = 3000L
    }
}
