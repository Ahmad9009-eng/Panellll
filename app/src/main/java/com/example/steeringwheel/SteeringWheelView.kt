package com.example.steeringwheel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class SteeringWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var steeringValue = 0f // -100 to +100
    
    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1a1a1a")
        style = Paint.Style.FILL
    }
    
    private val wheelBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00d4ff")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ff0066")
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }
    
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00d4ff")
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }
    
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    fun setSteering(value: Float) {
        steeringValue = value.coerceIn(-100f, 100f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val wheelRadius = minOf(width, height) / 2.5f
        
        // Draw background
        canvas.drawColor(Color.parseColor("#0a0a0a"))
        
        // Draw grid pattern
        drawGrid(canvas, centerX, centerY)
        
        // Draw wheel circle
        canvas.drawCircle(centerX, centerY, wheelRadius, wheelPaint)
        canvas.drawCircle(centerX, centerY, wheelRadius, wheelBorderPaint)
        
        // Draw reference lines (0°, ±45°, ±90°)
        drawReferenceLines(canvas, centerX, centerY, wheelRadius)
        
        // Draw steering needle
        drawNeedle(canvas, centerX, centerY, wheelRadius)
        
        // Draw steering value text
        canvas.drawText("${steeringValue.toInt()}°", centerX, centerY + 60, textPaint)
        
        // Draw left/right labels
        canvas.drawText("LEFT", centerX - wheelRadius - 30, centerY + 10, smallTextPaint)
        canvas.drawText("RIGHT", centerX + wheelRadius + 30, centerY + 10, smallTextPaint)
        
        // Draw center indicator
        canvas.drawCircle(centerX, centerY, 15f, needlePaint)
    }
    
    private fun drawGrid(canvas: Canvas, centerX: Float, centerY: Float) {
        val gridSize = 50f
        
        // Vertical lines
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridSize
        }
        
        // Horizontal lines
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gridSize
        }
    }
    
    private fun drawReferenceLines(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#444444")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        // 0° (top)
        canvas.drawLine(centerX, centerY - radius, centerX, centerY - radius - 20, linePaint)
        
        // ±45° angles
        val angle45 = Math.PI / 4
        val x45Pos = centerX + (radius * sin(angle45)).toFloat()
        val y45Pos = centerY - (radius * cos(angle45)).toFloat()
        val x45Neg = centerX - (radius * sin(angle45)).toFloat()
        val y45Neg = centerY - (radius * cos(angle45)).toFloat()
        
        canvas.drawLine(x45Pos, y45Pos, 
            x45Pos + 10 * sin(angle45).toFloat(), 
            y45Pos - 10 * cos(angle45).toFloat(), linePaint)
        canvas.drawLine(x45Neg, y45Neg, 
            x45Neg - 10 * sin(angle45).toFloat(), 
            y45Neg - 10 * cos(angle45).toFloat(), linePaint)
    }
    
    private fun drawNeedle(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        // Convert steering value (-100 to +100) to angle
        // -100 = -90°, 0 = 0°, +100 = +90°
        val angleRadians = Math.toRadians((steeringValue / 100f * 90f).toDouble())
        
        val needleEndX = centerX + (radius * 0.8f * sin(angleRadians)).toFloat()
        val needleEndY = centerY - (radius * 0.8f * cos(angleRadians)).toFloat()
        
        // Draw needle line with gradient effect
        val needleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#ff0066")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }
        
        canvas.drawLine(centerX, centerY, needleEndX, needleEndY, needleLinePaint)
        
        // Draw needle tip
        canvas.drawCircle(needleEndX, needleEndY, 8f, needlePaint)
        
        // Draw directional indicators
        val indicatorX = centerX + (radius * 0.95f * sin(angleRadians)).toFloat()
        val indicatorY = centerY - (radius * 0.95f * cos(angleRadians)).toFloat()
        
        val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00ff00")
            style = Paint.Style.FILL
        }
        
        canvas.drawCircle(indicatorX, indicatorY, 5f, indicatorPaint)
    }
}
