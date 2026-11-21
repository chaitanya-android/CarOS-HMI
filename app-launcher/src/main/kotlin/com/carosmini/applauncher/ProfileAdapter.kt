package com.carosmini.applauncher

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.carosmini.core.Profile

class ProfileAdapter(
    private var profiles: List<Profile>,
    private val onSetDefault: (Profile) -> Unit,
    private val onRename: (Profile) -> Unit,
    private val onDelete: (Profile) -> Unit,
) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    companion object {
        val AVATAR_COLORS = intArrayOf(
            Color.parseColor("#F44336"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#009688"),
            Color.parseColor("#E91E63"),
        )
    }

    inner class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        private val viewAvatar: View = root.findViewById(R.id.view_avatar)
        private val tvName: TextView = root.findViewById(R.id.tv_name)
        private val tvDefault: TextView = root.findViewById(R.id.tv_default)
        private val btnSetDefault: Button = root.findViewById(R.id.btn_set_default)
        private val btnRename: Button = root.findViewById(R.id.btn_rename)
        private val btnDelete: Button = root.findViewById(R.id.btn_delete)

        fun bind(profile: Profile) {
            tvName.text = profile.name
            tvDefault.visibility = if (profile.isDefault) View.VISIBLE else View.GONE

            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(AVATAR_COLORS[profile.avatarResId % AVATAR_COLORS.size])
            }
            viewAvatar.background = shape

            btnSetDefault.isEnabled = !profile.isDefault
            btnSetDefault.setOnClickListener { onSetDefault(profile) }

            btnRename.setOnClickListener { onRename(profile) }

            // The default profile cannot be deleted — it must always exist.
            btnDelete.isEnabled = !profile.isDefault
            btnDelete.setOnClickListener { onDelete(profile) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(profiles[position])

    override fun getItemCount() = profiles.size

    fun update(newList: List<Profile>) {
        profiles = newList
        notifyDataSetChanged()
    }
}
