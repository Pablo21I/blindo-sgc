package com.example.blindo.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.blindo.data.local.dao.AsignacionEppDao;
import com.example.blindo.data.local.dao.EppDao;
import com.example.blindo.data.local.dao.OrdenActivaDao;
import com.example.blindo.data.local.dao.PersonalDao;
import com.example.blindo.data.local.dao.ReglaTrabajoEppDao;
import com.example.blindo.data.local.dao.RegistroAuditoriaDao;
import com.example.blindo.data.local.dao.TrabajoDao;
import com.example.blindo.data.local.dao.UsuarioDao;
import com.example.blindo.data.local.dao.ZonaDao;

import com.example.blindo.data.local.entity.AsignacionEppEntity;
import com.example.blindo.data.local.entity.EppEntity;
import com.example.blindo.data.local.entity.OrdenActivaEntity;
import com.example.blindo.data.local.entity.PersonalEntity;
import com.example.blindo.data.local.entity.ReglaTrabajoEppEntity;
import com.example.blindo.data.local.entity.RegistroAuditoriaEntity;
import com.example.blindo.data.local.entity.TrabajoEntity;
import com.example.blindo.data.local.entity.UsuarioEntity;
import com.example.blindo.data.local.entity.ZonaEntity;

@Database(
    entities = {
        UsuarioEntity.class,
        PersonalEntity.class,
        TrabajoEntity.class,
        EppEntity.class,
        ReglaTrabajoEppEntity.class,
        OrdenActivaEntity.class,
        RegistroAuditoriaEntity.class,
        AsignacionEppEntity.class,
        ZonaEntity.class
    },
    version = 3,
    exportSchema = false
)
public abstract class BlindoDatabase extends RoomDatabase {

    public abstract UsuarioDao usuarioDao();
    public abstract PersonalDao personalDao();
    public abstract TrabajoDao trabajoDao();
    public abstract EppDao eppDao();
    public abstract ReglaTrabajoEppDao reglaTrabajoEppDao();
    public abstract OrdenActivaDao ordenActivaDao();
    public abstract RegistroAuditoriaDao registroAuditoriaDao();
    public abstract AsignacionEppDao asignacionEppDao();
    public abstract ZonaDao zonaDao();

    private static volatile BlindoDatabase INSTANCE;

    public static BlindoDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BlindoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        BlindoDatabase.class,
                        "blindo_database.db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
