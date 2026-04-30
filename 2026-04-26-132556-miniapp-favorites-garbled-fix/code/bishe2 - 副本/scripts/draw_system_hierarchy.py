from PIL import Image, ImageDraw, ImageFont


WIDTH = 1500
HEIGHT = 1100
BG = "white"
LINE = "black"


def load_font(size: int, bold: bool = False):
    candidates = [
        "C:/Windows/Fonts/simhei.ttf",
        "C:/Windows/Fonts/msyh.ttc",
        "C:/Windows/Fonts/simsun.ttc",
        "C:/Windows/Fonts/arial.ttf",
    ]
    for path in candidates:
        try:
            return ImageFont.truetype(path, size=size)
        except OSError:
            continue
    return ImageFont.load_default()


FONT_TITLE = load_font(28)
FONT_LABEL = load_font(22)
FONT_TEXT = load_font(20)
FONT_SMALL = load_font(18)


def center_text(draw, box, text, font, fill=LINE):
    x1, y1, x2, y2 = box
    bbox = draw.multiline_textbbox((0, 0), text, font=font, align="center", spacing=4)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    tx = x1 + (x2 - x1 - tw) / 2
    ty = y1 + (y2 - y1 - th) / 2
    draw.multiline_text((tx, ty), text, font=font, fill=fill, align="center", spacing=4)


def rect(draw, box, text, font=FONT_TEXT, width=2):
    draw.rectangle(box, outline=LINE, width=width)
    center_text(draw, box, text, font)


def dashed_line(draw, y, x1=70, x2=1430, dash=16, gap=10):
    x = x1
    while x < x2:
        draw.line((x, y, min(x + dash, x2), y), fill=LINE, width=2)
        x += dash + gap


def arrow(draw, p1, p2, width=2, arrow_size=10):
    draw.line((p1, p2), fill=LINE, width=width)
    x1, y1 = p1
    x2, y2 = p2
    if x1 == x2 and y1 == y2:
        return
    import math
    angle = math.atan2(y2 - y1, x2 - x1)
    a1 = angle + math.pi * 5 / 6
    a2 = angle - math.pi * 5 / 6
    p3 = (x2 + arrow_size * math.cos(a1), y2 + arrow_size * math.sin(a1))
    p4 = (x2 + arrow_size * math.cos(a2), y2 + arrow_size * math.sin(a2))
    draw.polygon([p2, p3, p4], fill=LINE)


img = Image.new("RGB", (WIDTH, HEIGHT), BG)
draw = ImageDraw.Draw(img)

# Title
center_text(draw, (0, 20, WIDTH, 70), "社区二手交易系统层次架构图", FONT_TITLE)

# Layer labels and separators
layers = [
    ("数据层（CloudBase数据库）", 110),
    ("数据访问与服务层", 250),
    ("业务控制层（Express/云函数）", 410),
    ("表示层（多端界面）", 650),
    ("用户层", 900),
]
for label, y in layers:
    draw.text((30, y - 25), label, font=FONT_LABEL, fill=LINE)
    dashed_line(draw, y + 55)

# Top data layer
db_box = (560, 110, 940, 180)
rect(draw, db_box, "CloudBase\n数据库", FONT_LABEL)
draw.text((160, 115), "users、listings、messages、\nconversations、feedback 等数据", font=FONT_SMALL, fill=LINE, spacing=4)

# DAO/service layer
dao_box = (650, 270, 850, 330)
rect(draw, dao_box, "数据访问层\napi.js / cloudbase.js", FONT_TEXT)

# Business layer
controller_box = (610, 430, 890, 510)
rect(draw, controller_box, "业务控制层\nExpress 路由 / 云函数\nmp-auth.js、web-api.js、admin.js、service.js\nweixinAuthLogin / resetUserPassword", FONT_SMALL)

# View layer container
view_container = (250, 680, 1250, 860)
draw.rectangle(view_container, outline=LINE, width=2)
center_text(draw, (250, 645, 1250, 675), "表示层（前端界面与后台界面）", FONT_LABEL)

views = [
    ((290, 720, 470, 810), "Uni-app\n微信小程序端\nView"),
    ((520, 720, 700, 810), "用户 Web 端\nuser-web\nView"),
    ((750, 720, 930, 810), "管理后台\nEJS Views"),
    ((980, 720, 1160, 810), "客服工作台\nEJS Views"),
]
for box, text in views:
    rect(draw, box, text, FONT_TEXT)

# User layer
users = [
    ((300, 940, 430, 990), "普通用户1"),
    ((520, 940, 650, 990), "普通用户2"),
    ((740, 940, 870, 990), "管理员"),
    ((960, 940, 1090, 990), "客服人员"),
]
for box, text in users:
    rect(draw, box, text, FONT_SMALL)

# Horizontal role labels
draw.text((345, 900), "商品浏览 / 发布", font=FONT_SMALL, fill=LINE)
draw.text((565, 900), "会话消息", font=FONT_SMALL, fill=LINE)
draw.text((770, 900), "审核管理", font=FONT_SMALL, fill=LINE)
draw.text((985, 900), "客服处理", font=FONT_SMALL, fill=LINE)

# Main arrows
arrow(draw, (750, 180), (750, 270))
arrow(draw, (750, 330), (750, 430))
arrow(draw, (750, 510), (750, 720))

# Side arrows from business to views
for x in (380, 610, 840, 1070):
    arrow(draw, (750, 510), (x, 720))

# Requests from users to views
for (x1, y1, x2, y2), _ in users:
    cx = (x1 + x2) // 2
    if cx < 500:
        target_x = 380
    elif cx < 720:
        target_x = 610
    elif cx < 920:
        target_x = 840
    else:
        target_x = 1070
    arrow(draw, (cx, 940), (target_x, 810))

# Flow labels
draw.text((770, 215), "Write / Read", font=FONT_SMALL, fill=LINE)
draw.text((770, 370), "业务封装", font=FONT_SMALL, fill=LINE)
draw.text((845, 590), "Response", font=FONT_SMALL, fill=LINE)
draw.text((470, 610), "Request / Response", font=FONT_SMALL, fill=LINE)

# MVC-ish side note from code reality
draw.text((75, 470), "说明：系统采用多端 + Express + CloudBase 的混合架构，\n并保留部分云函数能力。", font=FONT_SMALL, fill=LINE, spacing=4)

# Footer caption
center_text(draw, (0, 1010, WIDTH, 1045), "图 3.1  系统层次架构图", FONT_LABEL)
center_text(draw, (0, 1045, WIDTH, 1080), "Figure 3.1  System hierarchy architecture", FONT_SMALL)

output = "G:/bishe2 - 副本/docs/system_hierarchy_generated.png"
img.save(output)
print(output)
