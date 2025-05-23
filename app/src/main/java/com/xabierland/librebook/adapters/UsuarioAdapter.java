package com.xabierland.librebook.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabierland.librebook.R;
import com.xabierland.librebook.activities.ProfileActivity;
import com.xabierland.librebook.data.database.entities.Usuario;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder> {
    
    private List<Usuario> usuarios;
    private Context context;
    
    public UsuarioAdapter(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }
    
    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_user, parent, false);
        return new UsuarioViewHolder(view);
    }
    
    // Modificar el método onBindViewHolder en com.xabierland.librebook.adapters.UsuarioAdapter.java
    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        
        holder.textViewNombre.setText(usuario.getNombre());
        holder.textViewEmail.setText(usuario.getEmail());
        
        // Formatear y mostrar la fecha de registro
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaFormateada = sdf.format(new Date(usuario.getFechaRegistro()));
        holder.textViewFechaRegistro.setText(String.format(context.getString(R.string.member_since), fechaFormateada));
                
        // Cargar foto de perfil si existe
        if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isEmpty()) {
            try {
                // Decodificar la cadena base64 a un bitmap
                byte[] decodedString = Base64.decode(usuario.getFotoPerfil(), Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.imageViewUserPhoto.setImageBitmap(decodedBitmap);
            } catch (Exception e) {
                Log.e("UsuarioAdapter", "Error al decodificar imagen base64", e);
                // Usar imagen por defecto en caso de error
                holder.imageViewUserPhoto.setImageResource(R.drawable.default_profile_image);
            }
        } else {
            // Usar imagen predeterminada
            holder.imageViewUserPhoto.setImageResource(R.drawable.default_profile_image);
        }
        
        // Configurar clic para abrir perfil de usuario
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra(ProfileActivity.EXTRA_USER_ID, usuario.getId());
            intent.putExtra(ProfileActivity.EXTRA_VIEW_ONLY, true);
            context.startActivity(intent);
        });
    }
    
    @Override
    public int getItemCount() {
        return usuarios != null ? usuarios.size() : 0;
    }
    
    static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageViewUserPhoto;
        TextView textViewNombre;
        TextView textViewEmail;
        TextView textViewFechaRegistro;
        
        UsuarioViewHolder(View itemView) {
            super(itemView);
            imageViewUserPhoto = itemView.findViewById(R.id.imageViewUserPhoto);
            textViewNombre = itemView.findViewById(R.id.textViewNombre);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewFechaRegistro = itemView.findViewById(R.id.textViewFechaRegistro);
        }
    }
}