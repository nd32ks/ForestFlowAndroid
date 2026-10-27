package com.chiforest.forestflow.feature.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.auth.LocalAuthRepository
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.Role as UserRole
import com.chiforest.forestflow.ui.components.BtnStyle
import com.chiforest.forestflow.ui.components.ForestButton
import com.chiforest.forestflow.ui.components.ForestCard
import com.chiforest.forestflow.ui.components.ForestSheet
import com.chiforest.forestflow.ui.components.Ic
import com.chiforest.forestflow.ui.components.LiveDot
import com.chiforest.forestflow.ui.components.LogoMark
import com.chiforest.forestflow.ui.components.Pill
import com.chiforest.forestflow.ui.components.SegmentedControl
import com.chiforest.forestflow.ui.components.SheetTitle
import com.chiforest.forestflow.ui.components.pressable
import com.chiforest.forestflow.ui.components.shake
import com.chiforest.forestflow.ui.components.softCard
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Type

@Composable
fun LoginScreen(s: AppState, now: Long, dispatch: (Action) -> Unit) {
    val c = FF.c
    val supplier = s.loginRole == UserRole.Supplier
    val codeLeft = s.codeLeftSec(now)
    Box(Modifier.fillMaxSize().background(c.bg)) {
        // ambient brand glow
        Canvas(Modifier.fillMaxWidth().height(420.dp)) {
            drawCircle(
                Brush.radialGradient(listOf(c.red2.copy(alpha = if (c.isDark) 0.22f else 0.16f), Color.Transparent), center = Offset(size.width * 0.2f, 40f), radius = size.width * 0.9f),
                radius = size.width * 0.9f, center = Offset(size.width * 0.2f, 40f),
            )
            drawCircle(
                Brush.radialGradient(listOf(c.blue.copy(alpha = 0.08f), Color.Transparent), center = Offset(size.width, 260f), radius = size.width * 0.6f),
                radius = size.width * 0.6f, center = Offset(size.width, 260f),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .widthIn(max = 430.dp)
                .align(Alignment.TopCenter),
        ) {
            Spacer(Modifier.height(28.dp))
            // brand header
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoMark(56)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("元气森林 Chi Forest", style = Type.display(26.sp, 800), color = c.ink)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("XJTLU Campus Node 01", style = Type.body(13.sp, 500), color = c.faint)
                        Spacer(Modifier.width(8.dp))
                        Pill("智选冷柜", c.red, c.redTint)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            SegmentedControl(
                listOf("学生 / 用户 Student", "补货供货商 Supplier"),
                if (supplier) 1 else 0,
                { dispatch(Action.SetRole(if (it == 0) UserRole.Student else UserRole.Supplier)) },
            )
            Spacer(Modifier.height(16.dp))

            // WeChat one-tap
            ForestCard(Modifier.fillMaxWidth(), radius = 24.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("推荐登录方式 / RECOMMENDED", style = Type.eyebrow(10.sp), color = c.faint, modifier = Modifier.weight(1f))
                    Pill("秒级连接", c.wechat, c.wechat.copy(alpha = 0.12f), icon = Ic.Bolt)
                }
                Spacer(Modifier.height(12.dp))
                ForestButton(
                    if (supplier) "微信登录 · Supplier WeChat" else "微信一键登录 · WeChat Login",
                    { dispatch(Action.Login("wechat")) },
                    Modifier.fillMaxWidth(), style = BtnStyle.WeChat, icon = Ic.Chat,
                    loading = s.loggingIn == "wechat",
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (supplier) "自动授权供应商工号与补货权限" else "自动授权西浦校园绑定的手机号与学号信息",
                    style = Type.body(12.sp), color = c.faint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(c.line))
                Text("或使用手机短信验证码登录", style = Type.body(12.sp, 500), color = c.faint, modifier = Modifier.padding(horizontal = 10.dp))
                Box(Modifier.weight(1f).height(1.dp).background(c.line))
            }

            // SMS card
            ForestCard(Modifier.fillMaxWidth(), radius = 24.dp) {
                Text("手机号码 / Mobile Number", style = Type.eyebrow(10.sp), color = c.faint)
                Spacer(Modifier.height(8.dp))
                FieldBox(Modifier.shake(s.shakeField == "phone", s.shakeNonce), error = s.shakeField == "phone") {
                    Text("+86", style = Type.num(15.sp, 700), color = c.ink)
                    Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(20.dp).background(c.line))
                    InputText(s.phone, { dispatch(Action.Phone(it)) }, "输入大陆手机号", KeyboardType.Phone, Modifier.weight(1f), "Mobile number")
                }
                Spacer(Modifier.height(12.dp))
                Text("短信验证码 / SMS Code", style = Type.eyebrow(10.sp), color = c.faint)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldBox(Modifier.weight(1f).shake(s.shakeField == "code", s.shakeNonce), error = s.shakeField == "code") {
                        Icon(Ic.Key, null, tint = c.faint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        InputText(s.smsCode, { dispatch(Action.Code(it)) }, "6 位验证码", KeyboardType.NumberPassword, Modifier.weight(1f), "SMS code", mono = true)
                    }
                    Spacer(Modifier.width(10.dp))
                    ForestButton(
                        if (codeLeft > 0) "${codeLeft}s" else "获取验证码",
                        { dispatch(Action.GetCode(System.currentTimeMillis())) },
                        Modifier.width(112.dp).height(50.dp), style = BtnStyle.Tonal, enabled = codeLeft == 0,
                    )
                }
                Spacer(Modifier.height(16.dp))
                ForestButton(
                    if (supplier) "登录补货后台 / Supplier Sign In" else "登录并解锁冷柜 / Sign In",
                    { dispatch(Action.Login("sms")) }, Modifier.fillMaxWidth(),
                    loading = s.loggingIn == "sms",
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AuthTile(Ic.Shield, "XJTLU CAS", "统一身份认证", Modifier.weight(1f), s.loggingIn == "cas") { dispatch(Action.Login("cas")) }
                AuthTile(Ic.Users, if (supplier) "临时 / 外包" else "访客 / 教职工", "临时通行通道", Modifier.weight(1f), s.loggingIn == "visitor") { dispatch(Action.Visitor) }
            }
            Spacer(Modifier.height(12.dp))

            // location banner
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface2).border(1.dp, c.line, RoundedCornerShape(18.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(c.redTint), contentAlignment = Alignment.Center) {
                    Icon(Ic.MapPin, null, tint = c.red, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("太仓校区教学主楼 G层 02号智能柜", style = Type.body(13.sp, 600), color = c.ink, maxLines = 1)
                    Text("· 离您 12 米", style = Type.body(12.sp), color = c.faint)
                }
                Pill("正常运行", c.green, c.greenTint, dot = true, pulse = true)
            }
            Spacer(Modifier.height(16.dp))

            // agreement
            val checkBg by animateColorAsState(if (s.agreed) c.red else Color.Transparent, label = "chk")
            Row(
                Modifier
                    .fillMaxWidth()
                    .shake(s.shakeField == "agree", s.shakeNonce)
                    .heightIn(min = 48.dp)
                    .semantics { stateDescription = if (s.agreed) "Checked" else "Not checked" }
                    .pressable(scaleTo = 0.98f, role = Role.Checkbox) { dispatch(Action.Agree) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(checkBg)
                        .border(1.5.dp, if (s.agreed) c.red else if (s.shakeField == "agree") c.red2 else c.faint, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (s.agreed) Icon(Ic.Check, null, tint = Color.White, modifier = Modifier.size(15.dp)) }
                Spacer(Modifier.width(10.dp))
                Text("我已阅读并同意《用户协议》与《隐私政策》 · I agree to the Terms & Privacy Policy", style = Type.body(12.sp), color = c.muted)
            }
            Spacer(Modifier.height(18.dp))
            Text("元气森林智能零售 · 微信小程序官方认证", style = Type.body(11.sp), color = c.faint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FieldBox(modifier: Modifier = Modifier, error: Boolean = false, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val c = FF.c
    val border by animateColorAsState(if (error) c.red2 else c.line, label = "fieldBorder")
    Row(
        modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).background(c.surface2)
            .border(1.dp, border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, content = content,
    )
}

@Composable
private fun InputText(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardType,
    modifier: Modifier = Modifier,
    description: String,
    mono: Boolean = false,
    password: Boolean = false,
) {
    val c = FF.c
    val style: TextStyle = (if (mono) Type.mono(16.sp, 700).copy(letterSpacing = 3.sp) else Type.num(15.sp, 600)).copy(color = c.ink)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(c.red),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Next),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier.semantics { contentDescription = description },
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = Type.body(14.sp), color = c.faint)
                inner()
            }
        },
    )
}

@Composable
private fun AuthTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, modifier: Modifier, loading: Boolean, onClick: () -> Unit) {
    val c = FF.c
    Row(
        modifier.softCard(RoundedCornerShape(20.dp)).pressable(scaleTo = 0.96f, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(c.blueTint), contentAlignment = Alignment.Center) {
            if (loading) androidx.compose.material3.CircularProgressIndicator(color = c.blue, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            else Icon(icon, null, tint = c.blue, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = Type.title(14.sp, 700), color = c.ink, maxLines = 1)
            Text(sub, style = Type.body(11.sp), color = c.faint, maxLines = 1)
        }
    }
}

/** XJTLU CAS: real email + password auth against the active AuthRepository (SQLite or Firebase). */
@Composable
fun CasSheet(s: AppState, backend: String, onSubmit: (String, String, Boolean, String) -> Unit, dispatch: (Action) -> Unit) {
    val c = FF.c
    val student = s.loginRole == UserRole.Student
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var create by rememberSaveable { mutableStateOf(false) }
    ForestSheet(onDismiss = { dispatch(Action.CloseSheet) }) {
        SheetTitle(
            if (create) "Create account" else "XJTLU CAS",
            "统一身份认证 · ${if (student) "Student" else "Supplier"} · $backend",
        ) { dispatch(Action.CloseSheet) }
        Column(Modifier.shake(s.shakeField == "cas", s.shakeNonce)) {
            AnimatedVisibility(create, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    FieldBox {
                        Icon(Ic.User, null, tint = c.faint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        InputText(name, { name = it }, "Full name", KeyboardType.Text, Modifier.weight(1f), "Full name")
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            FieldBox(error = s.casError != null) {
                Icon(Ic.Mail, null, tint = c.faint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                InputText(email, { email = it.trim() }, "you@xjtlu.edu.cn", KeyboardType.Email, Modifier.weight(1f), "Email")
            }
            Spacer(Modifier.height(10.dp))
            FieldBox(error = s.casError != null) {
                Icon(Ic.Lock, null, tint = c.faint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                InputText(password, { password = it }, "Password", KeyboardType.Password, Modifier.weight(1f), "Password", password = true)
            }
        }
        AnimatedVisibility(s.casError != null) {
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Ic.Alert, null, tint = c.red, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.casError ?: "", style = Type.body(13.sp, 600), color = c.red)
            }
        }
        Spacer(Modifier.height(16.dp))
        ForestButton(
            if (create) "Create ${if (student) "Student" else "Supplier"} account" else "Sign in",
            { onSubmit(email, password, create, name) }, Modifier.fillMaxWidth(), loading = s.casBusy,
        )
        Spacer(Modifier.height(8.dp))
        ForestButton(
            if (create) "I already have an account" else "New here? Create account",
            { create = !create }, Modifier.fillMaxWidth(), style = BtnStyle.Ghost,
        )
        if (!create) {
            Spacer(Modifier.height(8.dp))
            val demo = if (student) LocalAuthRepository.DEMO_STUDENT else LocalAuthRepository.DEMO_SUPPLIER
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface2)
                    .pressable(scaleTo = 0.98f, label = "Fill demo account") {
                        email = demo
                        password = LocalAuthRepository.DEMO_PASSWORD
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveDot(c.green, size = 7.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Demo account · tap to fill", style = Type.body(12.sp, 700), color = c.ink)
                    Text("$demo  ·  ${LocalAuthRepository.DEMO_PASSWORD}", style = Type.mono(12.sp, 500), color = c.faint)
                }
            }
        }
        val agreeAlpha by animateFloatAsState(if (s.agreed) 0f else 1f, label = "agreeHint")
        if (agreeAlpha > 0f) {
            Spacer(Modifier.height(8.dp))
            Text("Tip: tick the agreement on the login screen first.", style = Type.body(11.sp), color = c.faint.copy(alpha = agreeAlpha))
        }
    }
}

