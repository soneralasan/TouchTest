package com.example.touchscreentest // Kendi paket adınla değiştir

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Tam ekran (Full Screen) moduna al
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val frameLayout = FrameLayout(this)
        val touchTestView = TouchTestView(this)
        frameLayout.addView(touchTestView)

        // Çizimleri temizlemek için bir buton
        val clearButton = Button(this).apply {
            text = "Ekranı Temizle"
            setBackgroundColor(Color.DKGRAY)
            setTextColor(Color.WHITE)
            setOnClickListener { touchTestView.clearCanvas() }
        }
        
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 50, 50, 0)
        }
        
        frameLayout.addView(clearButton, params)
        setContentView(frameLayout)
    }
}

// Dokunmatik algılama ve çizim işlemlerini yapan özel View sınıfı
class TouchTestView(context: Context) : View(context) {

    private val activePointers = mutableMapOf<Int, PointF>()
    private val activePaths = mutableMapOf<Int, Path>()
    private val completedPaths = mutableListOf<Path>()

    // Renk paleti (farklı parmaklar için farklı renkler)
    private val pointerColors = arrayOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN,
        Color.YELLOW, Color.parseColor("#FFA500"), // Turuncu
        Color.parseColor("#800080"), // Mor
        Color.parseColor("#008080"), // Teal
        Color.DKGRAY
    )

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 45f
        isAntiAlias = true
        setShadowLayer(2f, 1f, 1f, Color.WHITE) // Okunabilirliği artırmak için gölge
    }

    private val touchCirclePaint = Paint().apply {
        style = Paint.Style.FILL
        alpha = 150 // Yarı saydam
    }

    fun clearCanvas() {
        completedPaths.clear()
        activePaths.clear()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                activePointers[pointerId] = PointF(x, y)
                
                val path = Path().apply { moveTo(x, y) }
                activePaths[pointerId] = path
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerCount = event.pointerCount
                for (i in 0 until pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    
                    activePointers[id]?.set(x, y)
                    activePaths[id]?.lineTo(x, y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                activePointers.remove(pointerId)
                activePaths[pointerId]?.let { 
                    completedPaths.add(it) 
                }
                activePaths.remove(pointerId)
            }
        }
        invalidate() // Ekranı yeniden çizmeye zorla
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.LTGRAY) // Arka plan rengi

        // Ekran çözünürlüğünü algıla ve yazdır
        canvas.drawText("Algılanan Ekran Boyutu: ${width}x${height}", 50f, 60f, textPaint)
        canvas.drawText("Mevcut Dokunma Sayısı: ${activePointers.size} / 10", 50f, 120f, textPaint)

        // Tamamlanmış serbest çizimleri siyah renk ile çiz
        linePaint.color = Color.BLACK
        for (path in completedPaths) {
            canvas.drawPath(path, linePaint)
        }

        // Aktif dokunma noktalarını, koordinatları ve aktif çizimleri çiz
        var textYOffset = 180f
        
        for ((id, point) in activePointers) {
            val colorIndex = id % pointerColors.size
            val currentColor = pointerColors[colorIndex]

            // Aktif çizim yolu
            activePaths[id]?.let { path ->
                linePaint.color = currentColor
                canvas.drawPath(path, linePaint)
            }

            // Parmak dokunma dairesi
            touchCirclePaint.color = currentColor
            canvas.drawCircle(point.x, point.y, 80f, touchCirclePaint)
            
            // Dairenin ortasına nokta
            touchCirclePaint.color = Color.BLACK
            canvas.drawCircle(point.x, point.y, 10f, touchCirclePaint)

            // Koordinat listeleme metni
            val coordinateText = "Parmak ID $id: X = ${point.x.toInt()}, Y = ${point.y.toInt()}"
            textPaint.color = currentColor
            canvas.drawText(coordinateText, 50f, textYOffset, textPaint)
            textYOffset += 60f
        }
    }
}
