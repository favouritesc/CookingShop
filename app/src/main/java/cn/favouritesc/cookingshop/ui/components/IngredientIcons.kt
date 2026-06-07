package cn.favouritesc.cookingshop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ===== 分类图标数据 =====
data class IconCategory(
    val name: String,
    val icons: List<String>
)

fun allIconCategories(): List<IconCategory> = listOf(
    IconCategory("蔬菜", listOf(
        "🥬","🥒","🥦","🥔","🥕","🌽","🍅","🍆","🫑","🌶️",
        "🧅","🧄","🫚","🥑","🍄","🫛","🫘","🌱","🎋","🪷",
        "🥗","🥜","🌰","🥬"
    )),
    IconCategory("肉类", listOf(
        "🥩","🍗","🍖","🥓","🌭","🍔","🥪","🌮","🌯",
        "🍗","🍖","🥓","🥩","🦴"
    )),
    IconCategory("水产海鲜", listOf(
        "🐟","🐠","🐡","🦐","🦞","🦀","🦑","🐙","🦪","🦞"
    )),
    IconCategory("主食面点", listOf(
        "🍚","🍜","🍝","🍛","🍲","🥟","🍞","🥖","🥐","🧇",
        "🥞","🍕","🌭","🍔","🥪","🧈"
    )),
    IconCategory("蛋奶豆制品", listOf(
        "🥚","🥛","🧀","🧈","🍳","🧋","🥤","🫘","🫛"
    )),
    IconCategory("水果", listOf(
        "🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍒",
        "🍑","🥭","🍍","🥥","🥝","🍅","🥑","🫒"
    )),
    IconCategory("饮品", listOf(
        "🥛","☕","🍵","🧃","🥤","🧋","🍶","🍺","🍷","🍸",
        "🍹","🧉","🧊"
    )),
    IconCategory("调味料", listOf(
        "🧂","🫙","🍯","🧈","🫒","🧉","🧊"
    )),
    IconCategory("甜品零食", listOf(
        "🍰","🎂","🧁","🍪","🍩","🍫","🍬","🍭","🍮","🍯",
        "🍿","🥨","🥠","🍘"
    )),
    IconCategory("其他常用", listOf(
        "🔥","💧","⭐","❤️","✨","🧊","🫧","⚡","☀️","🌈",
        "🍽️","🥢","🔪","🫗"
    ))
)

// 扁平化所有图标（去重）
fun allAvailableIcons(): List<String> =
    allIconCategories().flatMap { it.icons }.distinct()

// ===== 食材名 → 图标映射（150+ 条目）=====
private val IconMap = mapOf(
    // === 蔬菜 ===
    "白菜" to "🥬", "娃娃菜" to "🥬", "油菜" to "🥬", "生菜" to "🥬", "菠菜" to "🥬",
    "油麦菜" to "🥬", "茼蒿" to "🥬", "芥蓝" to "🥬", "空心菜" to "🥬", "苋菜" to "🥬",
    "卷心菜" to "🥬", "包菜" to "🥬", "紫甘蓝" to "🥬", "羽衣甘蓝" to "🥬",
    "芹菜" to "🥬", "西芹" to "🥬", "韭菜" to "🥬", "香菜" to "🌿", "蒜苗" to "🌿", "蒜苔" to "🌿",
    "葱" to "🧅", "大葱" to "🧅", "小葱" to "🧅", "香葱" to "🧅", "洋葱" to "🧅",
    "蒜" to "🧄", "大蒜" to "🧄", "姜" to "🫚", "生姜" to "🫚", "老姜" to "🫚",
    "辣椒" to "🌶️", "青椒" to "🫑", "红椒" to "🌶️", "小米辣" to "🌶️", "干辣椒" to "🌶️",
    "尖椒" to "🫑", "柿子椒" to "🫑", "彩椒" to "🫑", "甜椒" to "🫑",
    "西红柿" to "🍅", "番茄" to "🍅", "圣女果" to "🍅",
    "土豆" to "🥔", "马铃薯" to "🥔", "玉米" to "🌽", "红薯" to "🍠", "紫薯" to "🍠", "山药" to "🥔",
    "萝卜" to "🥕", "胡萝卜" to "🥕", "白萝卜" to "🥕", "红萝卜" to "🥕", "水萝卜" to "🥕",
    "黄瓜" to "🥒", "冬瓜" to "🍈", "南瓜" to "🎃", "苦瓜" to "🥒", "丝瓜" to "🥒", "西葫芦" to "🥒",
    "茄子" to "🍆", "菜花" to "🥦", "花菜" to "🥦", "西兰花" to "🥦",
    "豆角" to "🫘", "四季豆" to "🫘", "豇豆" to "🫘", "扁豆" to "🫘", "荷兰豆" to "🫛",
    "豌豆" to "🫛", "毛豆" to "🫛", "豆芽" to "🌱", "黄豆芽" to "🌱", "绿豆芽" to "🌱",
    "莲藕" to "🪷", "藕" to "🪷", "芋头" to "🥔", "竹笋" to "🎋", "冬笋" to "🎋", "春笋" to "🎋",
    "芦笋" to "🌿", "莴笋" to "🥒", "秋葵" to "🫛", "菱角" to "🌰",
    "木耳" to "🫘", "银耳" to "🫘", "榛蘑" to "🍄", "茶树菇" to "🍄",
    "香菇" to "🍄", "蘑菇" to "🍄", "金针菇" to "🍄", "杏鲍菇" to "🍄", "平菇" to "🍄", "口蘑" to "🍄",
    "海带" to "🌿", "紫菜" to "🌿", "裙带菜" to "🌿", "海藻" to "🌿",
    "牛油果" to "🥑", "橄榄" to "🫒",

    // === 肉类 ===
    "猪肉" to "🥩", "牛肉" to "🥩", "羊肉" to "🥩", "鸡肉" to "🍗", "鸭肉" to "🦆", "鹅肉" to "🦆",
    "排骨" to "🍖", "五花肉" to "🥓", "里脊" to "🥩", "肉末" to "🥩", "肉丝" to "🥩", "肉片" to "🥩",
    "培根" to "🥓", "火腿" to "🥓", "香肠" to "🌭", "腊肉" to "🥓", "腊肠" to "🌭",
    "鸡翅" to "🍗", "鸡腿" to "🍗", "鸡胸" to "🍗", "鸡爪" to "🍗", "鸡胗" to "🍗",
    "鸭脖" to "🦆", "鸭翅" to "🦆", "鸭舌" to "🦆",
    "牛腩" to "🥩", "牛腱" to "🥩", "肥牛" to "🥩", "牛尾" to "🥩", "牛肚" to "🥩",
    "羊排" to "🥩", "羊腿" to "🥩",
    "骨头" to "🦴", "棒骨" to "🦴",

    // === 水产 ===
    "鱼肉" to "🐟", "鱼片" to "🐟", "三文鱼" to "🐟", "金枪鱼" to "🐟", "带鱼" to "🐟",
    "鲈鱼" to "🐟", "鲫鱼" to "🐟", "鲤鱼" to "🐟", "草鱼" to "🐟", "黄鱼" to "🐟",
    "鳕鱼" to "🐟", "鲳鱼" to "🐟", "龙利鱼" to "🐟", "巴沙鱼" to "🐟",
    "虾" to "🦐", "虾仁" to "🦐", "大虾" to "🦐", "基围虾" to "🦐", "小龙虾" to "🦞",
    "蟹" to "🦀", "螃蟹" to "🦀", "大闸蟹" to "🦀", "帝王蟹" to "🦀",
    "鱿鱼" to "🦑", "墨鱼" to "🦑", "章鱼" to "🐙",
    "贝" to "🦪", "扇贝" to "🦪", "蛤蜊" to "🦪", "蛏子" to "🦪", "牡蛎" to "🦪", "生蚝" to "🦪",
    "鲍鱼" to "🦪", "海参" to "🦪", "海蜇" to "🦪",
    "龙虾" to "🦞", "皮皮虾" to "🦐", "面包蟹" to "🦀",

    // === 蛋奶 ===
    "鸡蛋" to "🥚", "鸭蛋" to "🥚", "鹌鹑蛋" to "🥚", "咸鸭蛋" to "🥚", "皮蛋" to "🥚",
    "牛奶" to "🥛", "酸奶" to "🥛", "奶酪" to "🧀", "芝士" to "🧀", "黄油" to "🧈", "奶油" to "🧈",
    "炼乳" to "🥛",

    // === 豆制品 ===
    "豆腐" to "🧈", "豆皮" to "🧈", "豆干" to "🧈", "千张" to "🧈", "腐竹" to "🧈", "素鸡" to "🧈",
    "豆泡" to "🧈", "豆腐皮" to "🧈", "豆浆" to "🥛",

    // === 主食 ===
    "米饭" to "🍚", "面条" to "🍜", "馒头" to "🥟", "饺子" to "🥟", "包子" to "🥟", "馄饨" to "🥟",
    "面包" to "🍞", "吐司" to "🍞", "法棍" to "🥖", "牛角包" to "🥐",
    "粉丝" to "🍜", "粉条" to "🍜", "米粉" to "🍜", "河粉" to "🍜", "宽粉" to "🍜",
    "意大利面" to "🍝", "方便面" to "🍜", "拉面" to "🍜", "刀削面" to "🍜",
    "粥" to "🍲", "稀饭" to "🍲", "汤" to "🍲", "羹" to "🍲",
    "煎饼" to "🥞", "手抓饼" to "🥞", "葱油饼" to "🥞", "油条" to "🥖",
    "粽子" to "🍘", "年糕" to "🍘", "汤圆" to "🍡", "元宵" to "🍡",

    // === 调料 ===
    "盐" to "🧂", "糖" to "🍬", "白糖" to "🍬", "冰糖" to "🍬", "红糖" to "🍬",
    "酱油" to "🫙", "生抽" to "🫙", "老抽" to "🫙", "醋" to "🫙", "白醋" to "🫙", "陈醋" to "🫙",
    "料酒" to "🍶", "黄酒" to "🍶", "蚝油" to "🫙", "鱼露" to "🫙",
    "豆瓣酱" to "🫙", "辣椒酱" to "🌶️", "番茄酱" to "🍅", "甜面酱" to "🫙",
    "味精" to "🧂", "鸡精" to "🧂", "胡椒粉" to "🧂", "花椒" to "🟤", "八角" to "⭐",
    "桂皮" to "🟤", "香叶" to "🌿", "丁香" to "🟤", "草果" to "🟤",
    "芝麻" to "🟤", "芝麻酱" to "🫙", "花生酱" to "🫙",
    "香油" to "🫒", "食用油" to "🫒", "橄榄油" to "🫒", "菜籽油" to "🫒", "花生油" to "🫒",
    "蜂蜜" to "🍯",

    // === 水果 ===
    "苹果" to "🍎", "香蕉" to "🍌", "橘子" to "🍊", "橙子" to "🍊", "葡萄" to "🍇",
    "草莓" to "🍓", "西瓜" to "🍉", "桃子" to "🍑", "梨" to "🍐",
    "柠檬" to "🍋", "芒果" to "🥭", "菠萝" to "🍍", "樱桃" to "🍒", "猕猴桃" to "🥝",
    "蓝莓" to "🫐", "石榴" to "🍎", "柚子" to "🍊", "哈密瓜" to "🍈", "荔枝" to "🍒",
    "榴莲" to "🫒", "椰子" to "🥥", "火龙果" to "🐉", "山竹" to "🍑", "枇杷" to "🍑",
    "李子" to "🍑", "杏" to "🍑", "枣" to "🫘", "山楂" to "🍒", "桑葚" to "🫐",

    // === 干货坚果 ===
    "花生" to "🥜", "核桃" to "🥜", "杏仁" to "🥜", "腰果" to "🥜", "开心果" to "🥜",
    "红枣" to "🫘", "枸杞" to "🫘", "莲子" to "🫘", "百合" to "🫘", "银耳" to "🫘",
    "桂圆" to "🫘", "枸杞子" to "🫘",
    "葡萄干" to "🍇", "梅干" to "🫘",

    // === 饮品 ===
    "茶" to "🍵", "绿茶" to "🍵", "红茶" to "🍵", "咖啡" to "☕", "啤酒" to "🍺",
    "白酒" to "🍶", "红酒" to "🍷", "果汁" to "🧃", "汽水" to "🥤", "可乐" to "🥤",
    "奶茶" to "🧋",

    // === 甜品 ===
    "蛋糕" to "🍰", "冰淇淋" to "🍨", "巧克力" to "🍫", "饼干" to "🍪", "布丁" to "🍮",
    "糖果" to "🍬", "爆米花" to "🍿",

    // === 其他 ===
    "水" to "💧", "冰块" to "🧊", "冰" to "🧊",
    "沙拉" to "🥗", "三明治" to "🥪", "汉堡" to "🍔", "披萨" to "🍕",
    "火锅" to "🍲", "烧烤" to "🔥", "串" to "🍢",
)

// 图标底色池
private val IconBgColors = listOf(
    Color(0xFFFFF3E0), Color(0xFFE8F5E9), Color(0xFFE3F2FD),
    Color(0xFFFCE4EC), Color(0xFFF3E5F5), Color(0xFFE0F7FA),
    Color(0xFFFFF8E1), Color(0xFFEFEBE9), Color(0xFFE8EAF6),
    Color(0xFFF1F8E9), Color(0xFFFFEBEE), Color(0xFFE0F2F1),
)

/** 根据食材获取图标：优先使用存储的 icon，否则自动匹配 */
fun ingredientIcon(name: String, storedIcon: String? = null): String {
    if (!storedIcon.isNullOrBlank()) return storedIcon
    // 精确匹配
    IconMap[name]?.let { return it }
    // 包含匹配（取最长匹配）
    val matched = IconMap.entries
        .filter { name.contains(it.key) }
        .maxByOrNull { it.key.length }
    if (matched != null) return matched.value
    // 首字作为 fallback
    return if (name.isNotEmpty()) name.take(1) else "?"
}

fun ingredientIconBgColor(name: String): Color {
    val index = kotlin.math.abs(name.hashCode()) % IconBgColors.size
    return IconBgColors[index]
}

@Composable
fun IngredientIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 32 /* dp */,
    storedIcon: String? = null
) {
    val icon = ingredientIcon(name, storedIcon)
    val isEmoji = icon.length == 1 || (icon.length >= 2 && icon[0].code > 255)

    if (isEmoji) {
        val bgColor = ingredientIconBgColor(name)
        Box(
            modifier = modifier.size(size.dp).clip(CircleShape).background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = (size * 0.5f).sp, textAlign = TextAlign.Center)
        }
    } else {
        val bgColor = ingredientIconBgColor(name)
        Box(
            modifier = modifier.size(size.dp).clip(CircleShape).background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = (size * 0.45f).sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

/** 返回 12 个默认常用食材名称 */
fun defaultIngredientNames(): List<String> = listOf(
    "白菜", "猪肉", "鸡蛋", "土豆", "西红柿",
    "葱", "姜", "蒜", "盐", "酱油", "食用油", "大米"
)
