package com.carosmini.applauncher

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.carosmini.core.Profile
import com.carosmini.core.ProfileServiceConnection

class ProfileManageActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: ProfileAdapter
    private var isServiceReady = false

    private val profileConn = ProfileServiceConnection { _ ->
        isServiceReady = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_manage)

        adapter = ProfileAdapter(
            profiles = emptyList(),
            onSetDefault = { profile -> setDefault(profile) },
            onRename = { profile -> showRenameDialog(profile) },
            onDelete = { profile -> confirmDelete(profile) },
        )

        val rv = findViewById<RecyclerView>(R.id.rv_profiles)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<Button>(R.id.btn_add_profile).setOnClickListener { showAddDialog() }

        profileConn.bind(this)
    }

    override fun onResume() {
        super.onResume()
        if (isServiceReady) {
            window.decorView.postDelayed({
                try {
                    loadProfiles()
                } catch (e: DeadObjectException) {
                    Log.e("SERVICE_DEBUG", "Binder died, rebinding", e)
                    isServiceReady = false
                    profileConn.bind(this)
                }
            }, 200)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        profileConn.unbind(this)
    }

    private fun loadProfiles() {
        val list = profileConn.service?.profiles ?: emptyList()
        runOnUiThread { adapter.update(list) }
    }

    private fun setDefault(profile: Profile) {
        profileConn.service?.setActiveProfile(profile.id)
        handler.postDelayed({ loadProfiles() }, 300)
    }

    private fun confirmDelete(profile: Profile) {
        AlertDialog.Builder(this)
            .setTitle("Delete \"${profile.name}\"?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                profileConn.service?.deleteProfile(profile.id)
                handler.postDelayed({ loadProfiles() }, 300)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(profile: Profile) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_profile_name)
        val colorRow = dialogView.findViewById<LinearLayout>(R.id.ll_colors)

        etName.setText(profile.name)
        etName.setSelection(profile.name.length)

        var selectedColor = profile.avatarResId
        val circles = buildColorCircles(colorRow, selectedColor) { idx -> selectedColor = idx }
        updateColorSelection(circles, selectedColor)

        AlertDialog.Builder(this)
            .setTitle("Rename profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                profileConn.service?.updateProfile(
                    profile.copy(name = name, avatarResId = selectedColor)
                )
                handler.postDelayed({ loadProfiles() }, 300)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_profile_name)
        val colorRow = dialogView.findViewById<LinearLayout>(R.id.ll_colors)

        var selectedColor = 0
        val circles = buildColorCircles(colorRow, selectedColor) { idx -> selectedColor = idx }
        updateColorSelection(circles, selectedColor)

        AlertDialog.Builder(this)
            .setTitle("New profile")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                profileConn.service?.createProfile(
                    Profile(name = name, avatarResId = selectedColor)
                )
                handler.postDelayed({ loadProfiles() }, 300)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildColorCircles(
        colorRow: LinearLayout,
        initialSelection: Int,
        onSelected: (Int) -> Unit,
    ): List<View> {
        val circles = mutableListOf<View>()
        val size = resources.getDimensionPixelSize(R.dimen.avatar_circle_size)
        val margin = resources.getDimensionPixelSize(R.dimen.avatar_circle_margin)

        ProfileAdapter.AVATAR_COLORS.forEachIndexed { index, color ->
            val circle = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = margin }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                setOnClickListener {
                    onSelected(index)
                    updateColorSelection(circles, index)
                }
            }
            circles.add(circle)
            colorRow.addView(circle)
        }
        return circles
    }

    private fun updateColorSelection(circles: List<View>, selectedIndex: Int) {
        val strokePx = resources.getDimensionPixelSize(R.dimen.avatar_stroke_width)
        circles.forEachIndexed { i, view ->
            (view.background as GradientDrawable).setStroke(
                if (i == selectedIndex) strokePx else 0,
                if (i == selectedIndex) Color.WHITE else Color.TRANSPARENT,
            )
        }
    }
}
