package cn.favouritesc.cookingshop.data.export

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object ExportManager {
    fun exportToCsv(
        context: Context,
        orderDate: String,
        orderTime: String,
        dishes: List<Triple<String, Int, String?>>
    ): Uri {
        val fileName = "order_${orderDate}_${orderTime.replace(":", "-")}.csv"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.write("\uFEFF")
                writer.write("日期,时间,菜品名称,数量,备菜清单\n")
                dishes.forEach { (dishName, quantity, ingredients) ->
                    writer.write("$orderDate,$orderTime,\"$dishName\",$quantity,\"${ingredients ?: ""}\"\n")
                }
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun exportToImage(
        context: Context,
        bitmap: Bitmap
    ): Uri {
        val fileName = "order_${System.currentTimeMillis()}.png"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
