# M8 sertleştirme: release derlemesi minify + kaynak küçültme ile yapılır.
# Compose, Room, Hilt ve WorkManager kendi consumer-proguard kurallarını AAR içinde taşır;
# burada yalnız reflection'la bulunan (Class.forName) sınıflar için ek güvence eklenir.

# WorkManager, süreç yeniden başladığında işi worker'ın tam sınıf adından yeniden kurar
# (Class.forName). R8 bu adları değiştirirse kayıtlı iş çalışmaz.
-keep class com.portfoy.arkaplan.** extends androidx.work.ListenableWorker { *; }
-keep class com.portfoy.arkaplan.** extends androidx.work.CoroutineWorker { *; }

# Hilt'in ürettiği worker fabrika sınıfları, assisted injection ile reflection kullanır.
-keep class **_HiltModules { *; }
-keep class **_Factory { *; }

# Uygulamanın kendi model sınıfları (Room entity/POJO alan adları yansıtılarak eşlenir).
-keep class com.portfoy.model.** { *; }
-keep class com.portfoy.data.db.** { *; }
