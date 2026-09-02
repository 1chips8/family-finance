from __future__ import annotations

import copy
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
TEMPLATE = ROOT / "资料" / "软件项目设计报告2026年.docx"
OUTPUT_DIR = ROOT / "交付" / "个人报告"
SCREEN_DIR = OUTPUT_DIR / "截图"


REPORTS = {
    "01-组长-软件项目设计报告.docx": {
        "name": "[组长姓名]",
        "student_id": "[组长学号]",
        "role": "组长",
        "focus": "需求与架构、数据库、安全认证、家庭与资料、集成验收",
        "personal_tasks": [
            ("需求与架构", "把任务书转为 PRD、接口契约、权限矩阵和阶段计划"),
            ("数据库与配置", "设计四张业务表、约束、索引、Flyway 迁移和 demo Profile"),
            ("安全认证", "实现 Session、CSRF、BCrypt、停用会话过滤和统一 401/403"),
            ("家庭与资料", "实现注册登录、创建/加入家庭、邀请码、姓名与密码修改"),
            ("集成交付", "对齐前后端 DTO，组织自动化门禁、浏览器验收和 README"),
        ],
        "detail": "leader",
        "screens": [
            ("01-login.png", "图 5-1 登录页：未输入账号密码的真实系统界面"),
            ("04-members.png", "图 5-2 家庭成员与家庭通行证：家长可查看邀请码和维护成员"),
            ("06-profile.png", "图 5-3 个人资料与修改密码：密码更新后要求重新登录"),
        ],
        "summary": (
            "在本项目中，我负责将任务书约束落实为可运行的整体方案，重点完成数据库、安全认证、家庭入户和集成验收。"
            "最需要谨慎处理的是身份与家庭边界：后端不能相信前端提交的家庭标识，所有业务必须从 Session 当前用户推导家庭范围；"
            "邀请码加入还需要在事务中锁定家庭和成员编号序列。通过统一异常模型、数据库约束和服务层校验，系统能够稳定区分 401、403、业务冲突和输入错误。"
            "后续可改进之处是进一步补充报告截图标注和测试覆盖说明，但不扩展预算、通知或公网部署等课设范围外功能。"
        ),
    },
    "02-组员一-软件项目设计报告.docx": {
        "name": "[组员一姓名]",
        "student_id": "[组员一学号]",
        "role": "组员一",
        "focus": "成员、分类、收支、统计后端业务与测试",
        "personal_tasks": [
            ("成员管理", "成员列表、编号/姓名/角色/状态维护和最后家长保护"),
            ("分类管理", "系统分类只读、家庭分类新增/改名/停用/恢复"),
            ("收支管理", "分页筛选、增改删、普通成员本人范围和逻辑删除"),
            ("统计服务", "收入、支出、结余、趋势、构成、成员对比和最近流水"),
            ("后端测试", "验证权限、跨家庭隔离、校验规则、删除与统计精度"),
        ],
        "detail": "backend",
        "screens": [
            ("03-entries.png", "图 5-1 收支明细：支持类型、分类、成员和日期筛选"),
            ("04-members.png", "图 5-2 家庭成员：家长可编辑资料和停用成员"),
            ("05-categories.png", "图 5-3 收支分类：系统分类只读，家庭分类可维护"),
            ("02-dashboard.png", "图 5-4 仪表盘：后端统计结果形成 KPI、趋势和构成"),
        ],
        "summary": (
            "在本项目中，我负责成员、分类、收支和统计四个后端业务模块。实现过程中，核心工作不是生成常规 CRUD，"
            "而是让每一次对象访问同时满足当前家庭范围、当前角色权限和业务状态约束。流水保存采用 BigDecimal 和数据库 DECIMAL，"
            "删除采用逻辑删除，分类停用保留历史引用，统计统一排除已删除记录。最后家长保护和跨家庭对象拒绝是最重要的失败路径。"
            "后续可以增加更细的聚合 SQL 性能对比，但当前数据规模下实时统计更简单、可解释，也更符合课程设计边界。"
        ),
    },
    "03-组员二-软件项目设计报告.docx": {
        "name": "[组员二姓名]",
        "student_id": "[组员二学号]",
        "role": "组员二",
        "focus": "Vue 应用、页面交互、统计图表、响应式与前端测试",
        "personal_tasks": [
            ("前端基础", "Vue、Pinia、Router、Axios、类型模型和视觉 token"),
            ("认证动线", "登录、注册、入户、会话恢复、401/403/404 页面"),
            ("业务页面", "仪表盘、流水、成员、分类和个人资料页面"),
            ("响应式", "桌面表格、手机卡片、控件换行和无横向溢出"),
            ("前端测试", "路由守卫、角色显隐、表单联动、错误分流和构建门禁"),
        ],
        "detail": "frontend",
        "screens": [
            ("01-login.png", "图 5-1 登录页：品牌化认证入口和清晰表单层级"),
            ("02-dashboard.png", "图 5-2 仪表盘：时间范围、KPI、趋势和收支构成"),
            ("03-entries.png", "图 5-3 收支明细：筛选栏、表格和记一笔入口"),
            ("04-members.png", "图 5-4 家庭成员：家庭通行证与角色化操作"),
        ],
        "summary": (
            "在本项目中，我负责把后端接口组织为完整的 Vue 用户动线。前端通过 Pinia 恢复会话，通过路由守卫区分未登录、未入户和已入户状态，"
            "通过 Axios 拦截器分别处理 401 与 403。仪表盘把同一统计 DTO 映射为金额卡片、趋势图、构成和成员对比；"
            "流水页面在桌面使用表格，在手机使用卡片，保持主要操作一致。测试重点覆盖角色显隐、表单联动、密码更新退出和错误页面。"
            "后续可以按需进行图表按需加载以降低构建包体积，但现有 warning 不影响本机课程演示。"
        ),
    },
}


def set_east_asia_font(run, font_name: str) -> None:
    run.font.name = font_name
    run._element.get_or_add_rPr().get_or_add_rFonts().set(qn("w:eastAsia"), font_name)


def shade_cell(cell, fill: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_repeat_table_header(row) -> None:
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "true")
    tr_pr.append(tbl_header)


def keep_row_together(row) -> None:
    tr_pr = row._tr.get_or_add_trPr()
    cant_split = OxmlElement("w:cantSplit")
    tr_pr.append(cant_split)


def clear_paragraph(paragraph) -> None:
    p = paragraph._element
    for child in list(p):
        if child.tag != qn("w:pPr"):
            p.remove(child)


def replace_paragraph_text(paragraph, text: str) -> None:
    sample = copy.deepcopy(paragraph.runs[0]._element) if paragraph.runs else None
    clear_paragraph(paragraph)
    if sample is not None:
        sample.text = text
        paragraph._element.append(sample)
    else:
        paragraph.add_run(text)


def remove_body_after_cover(doc: Document) -> None:
    for paragraph in list(doc.paragraphs[8:]):
        paragraph._element.getparent().remove(paragraph._element)


def add_page_number(paragraph) -> None:
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = " PAGE "
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.extend([begin, instr, end])
    set_east_asia_font(run, "宋体")
    run.font.size = Pt(9)


def add_body(doc: Document, text: str, *, bold: bool = False, first_indent: bool = True) -> None:
    paragraph = doc.add_paragraph()
    paragraph.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    paragraph.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
    paragraph.paragraph_format.space_after = Pt(4)
    if first_indent:
        paragraph.paragraph_format.first_line_indent = Pt(22)
    run = paragraph.add_run(text)
    set_east_asia_font(run, "宋体")
    run.font.size = Pt(11)
    run.bold = bold


def add_heading(doc: Document, text: str, level: int = 1, page_break: bool = False) -> None:
    if page_break:
        doc.add_page_break()
    paragraph = doc.add_paragraph()
    paragraph.paragraph_format.keep_with_next = True
    paragraph.paragraph_format.space_before = Pt(10 if level == 1 else 7)
    paragraph.paragraph_format.space_after = Pt(6)
    run = paragraph.add_run(text)
    set_east_asia_font(run, "黑体")
    run.bold = True
    run.font.size = Pt(14 if level == 1 else 12)


def add_bullets(doc: Document, items: list[str]) -> None:
    for item in items:
        paragraph = doc.add_paragraph(style=None)
        paragraph.paragraph_format.left_indent = Pt(22)
        paragraph.paragraph_format.first_line_indent = Pt(-11)
        paragraph.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
        run = paragraph.add_run("• " + item)
        set_east_asia_font(run, "宋体")
        run.font.size = Pt(11)


def add_table(doc: Document, headers: list[str], rows: list[tuple[str, ...]], widths=None) -> None:
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    table.autofit = False
    if widths is None:
        widths = [Inches(5.95 / len(headers)) for _ in headers]
    for index, (cell, header, width) in enumerate(zip(table.rows[0].cells, headers, widths)):
        cell.width = width
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        shade_cell(cell, "E9EEF9")
        paragraph = cell.paragraphs[0]
        paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = paragraph.add_run(header)
        set_east_asia_font(run, "黑体")
        run.font.size = Pt(9.5)
        run.bold = True
    set_repeat_table_header(table.rows[0])
    keep_row_together(table.rows[0])
    for values in rows:
        row = table.add_row()
        keep_row_together(row)
        for cell, value, width in zip(row.cells, values, widths):
            cell.width = width
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            paragraph = cell.paragraphs[0]
            paragraph.paragraph_format.space_after = Pt(0)
            run = paragraph.add_run(str(value))
            set_east_asia_font(run, "宋体")
            run.font.size = Pt(9.5)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


def add_code(doc: Document, code: str) -> None:
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    cell = table.cell(0, 0)
    shade_cell(cell, "F5F7FA")
    paragraph = cell.paragraphs[0]
    paragraph.paragraph_format.space_before = Pt(4)
    paragraph.paragraph_format.space_after = Pt(4)
    for index, line in enumerate(code.strip("\n").splitlines()):
        run = paragraph.add_run(line)
        run.font.name = "Consolas"
        run._element.get_or_add_rPr().get_or_add_rFonts().set(qn("w:eastAsia"), "宋体")
        run.font.size = Pt(8.5)
        if index < len(code.strip("\n").splitlines()) - 1:
            run.add_break()


def add_picture(doc: Document, filename: str, caption: str) -> None:
    path = SCREEN_DIR / filename
    if not path.exists():
        add_body(doc, f"缺少截图：{path}", first_indent=False)
        return
    paragraph = doc.add_paragraph()
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph.paragraph_format.keep_with_next = True
    paragraph.add_run().add_picture(str(path), width=Inches(5.9))
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cap.paragraph_format.space_after = Pt(8)
    run = cap.add_run(caption)
    set_east_asia_font(run, "宋体")
    run.font.size = Pt(10)


def build_cover(doc: Document, report: dict) -> None:
    replace_paragraph_text(doc.paragraphs[0], "《软件项目设计》")
    replace_paragraph_text(doc.paragraphs[1], "题　　目：家庭理财系统的设计与实现")
    replace_paragraph_text(doc.paragraphs[2], "专业班级：[专业班级]")
    replace_paragraph_text(doc.paragraphs[3], f"学　　号：{report['student_id']}")
    replace_paragraph_text(doc.paragraphs[4], f"姓　　名：{report['name']}")
    replace_paragraph_text(doc.paragraphs[5], "指导教师：[指导教师]")
    replace_paragraph_text(doc.paragraphs[6], "计算机学院")
    replace_paragraph_text(doc.paragraphs[7], "2026 年 9 月")


def add_common_chapter_one(doc: Document, report: dict) -> None:
    add_heading(doc, "一、进度计划与可行性分析", level=1, page_break=True)
    add_heading(doc, "1.1 项目进度计划", level=2)
    add_body(doc, "项目按需求与架构、工程与数据库、认证与权限、核心业务、统计与前端、测试与报告六个阶段推进。各阶段先明确输入和验收条件，再进行实现与联调，避免在课设范围内引入没有当前需求的复杂设施。")
    add_table(
        doc,
        ["阶段", "主要工作", "主要产出", "状态"],
        [
            ("需求与架构", "任务书分析、角色与边界、接口和数据模型", "PRD、架构设计、实施计划", "完成"),
            ("工程与数据库", "前后端骨架、四表迁移、配置样例", "可启动工程、Flyway SQL", "完成"),
            ("认证与权限", "Session、CSRF、家庭入户、角色边界", "认证与家庭闭环", "完成"),
            ("核心业务", "成员、分类、流水和资料", "业务 CRUD 与校验", "完成"),
            ("统计与前端", "仪表盘、页面壳、响应式和状态页", "可演示 Web 系统", "完成"),
            ("测试与报告", "门禁、浏览器验收、文档和截图", "验收记录与个人报告", "进行中"),
        ],
        [Inches(1.05), Inches(2.05), Inches(1.85), Inches(0.9)],
    )
    add_heading(doc, "1.2 本人责任范围", level=2)
    add_body(doc, f"本人在小组中担任{report['role']}，主要负责{report['focus']}。具体任务如下：")
    add_table(doc, ["任务", "工作内容"], report["personal_tasks"], [Inches(1.35), Inches(4.55)])
    add_heading(doc, "1.3 可行性分析", level=2)
    add_body(doc, "技术可行性：Java 17、Spring Boot、Spring Security、MyBatis-Plus、Vue 3、Element Plus 和 ECharts 均有成熟工具链，前后端通过 JSON 接口解耦，适合课程设计规模。")
    add_body(doc, "经济可行性：项目使用开源框架和本机 MySQL，不需要云服务、域名、短信、支付或第三方付费接口，主要成本为小组开发和测试时间。")
    add_body(doc, "运行可行性：系统采用浏览器访问，用户只需完成登录、创建或加入家庭即可使用；README 提供精确启动命令和演示账号。")
    add_body(doc, "进度可行性：模块按接口边界分配，后端业务和前端页面可在统一契约下并行；最终由组长执行一次集成门禁和浏览器验收。")
    add_heading(doc, "1.4 技术现状与趋势", level=2)
    add_body(doc, "当前 Web 管理类系统普遍采用前后端分离、组件化界面、服务端统一鉴权和自动化构建。家庭记账类应用的发展重点从单纯流水记录转向多成员协作、可视化统计、隐私隔离和移动端体验。本课设选择其中最能体现软件工程能力的身份、权限、CRUD、统计和响应式功能，不接入真实金融账户。")


def add_common_chapter_two(doc: Document, report: dict) -> None:
    add_heading(doc, "二、需求分析", level=1, page_break=True)
    add_heading(doc, "2.1 用户角色", level=2)
    add_table(
        doc,
        ["角色", "主要目标", "权限摘要"],
        [
            ("未入户用户", "完成家庭归属", "注册登录后创建家庭或输入邀请码加入"),
            ("普通成员", "维护个人收支并查看家庭整体", "只能维护本人流水，可查看成员名单和家庭聚合统计"),
            ("家长", "管理家庭和全家收支", "维护成员、家庭分类、邀请码和全家流水"),
        ],
        [Inches(1.15), Inches(2.0), Inches(2.75)],
    )
    add_heading(doc, "2.2 功能需求", level=2)
    add_bullets(doc, [
        "认证：注册、登录、退出、刷新恢复、修改姓名与密码。",
        "家庭：创建家庭、邀请码加入、查看家庭信息和重置邀请码。",
        "成员：查看成员；家长修改成员编号、姓名、角色与状态。",
        "分类：查看系统/家庭分类；家长维护自定义分类。",
        "收支：按日期、类型、分类、成员分页筛选，新增、修改和逻辑删除。",
        "统计：总收入、总支出、结余、近 12 月趋势、类别构成、成员对比和最近流水。",
    ])
    add_heading(doc, "2.3 核心业务规则", level=2)
    add_bullets(doc, [
        "一个账号只属于一个家庭；创建者为家长，邀请码加入者为普通成员。",
        "所有家庭范围从当前 Session 用户取得，不接受客户端家庭 ID 作为可信来源。",
        "普通成员只能维护本人流水；最后一名活跃家长不能停用或降级。",
        "系统分类不可编辑；停用分类不能用于新流水，但历史数据保留。",
        "金额必须大于零并保留两位小数；类别类型必须与收入/支出类型一致；日期不晚于当天。",
        "流水逻辑删除后立即从列表和统计中排除。",
    ])
    add_heading(doc, "2.4 非功能需求", level=2)
    add_body(doc, "安全方面使用 BCrypt、服务端 Session、CSRF、统一鉴权和家庭隔离；可靠性方面使用事务、外键、唯一索引和可恢复状态；易用性方面提供加载、空数据、字段错误、403、404 和重试状态；兼容性方面以桌面 Chrome/Edge 为主并适配 390px 手机宽度。")
    add_heading(doc, "2.5 本人模块验收标准", level=2)
    add_body(doc, f"本人负责的{report['focus']}必须与统一接口契约一致，既能完成正常动线，也能正确拒绝越权、无效输入和冲突操作；相关源码、测试、构建和最终页面应相互对应。")


def add_common_chapter_three(doc: Document) -> None:
    add_heading(doc, "三、总体设计", level=1, page_break=True)
    add_heading(doc, "3.1 开发工具与运行平台", level=2)
    add_table(
        doc,
        ["层次", "技术", "选择理由"],
        [
            ("前端", "Vue 3、TypeScript、Vite", "组件化、类型约束和快速开发构建"),
            ("界面", "Element Plus、ECharts、Lucide", "表单表格、统计图和统一图标"),
            ("后端", "Java 17、Spring Boot", "成熟的 Web、校验、事务和测试生态"),
            ("安全", "Spring Security、Session、CSRF", "符合本机浏览器会话场景"),
            ("持久化", "MyBatis-Plus、Flyway", "简化 CRUD 并保证数据库可复现"),
            ("数据库", "MySQL 8", "支持事务、外键、索引和精确小数"),
        ],
        [Inches(1.0), Inches(2.1), Inches(2.8)],
    )
    add_heading(doc, "3.2 总体架构", level=2)
    add_body(doc, "系统采用单体后端加单页前端的 B/S 架构。浏览器加载 Vue 应用，Axios 通过 Vite 开发代理访问 `/api`；Spring MVC Controller 接收请求，Spring Security 维护 Session 与 CSRF，Service 处理权限和业务，Mapper 访问 MySQL，Flyway 在启动时维护 Schema 版本。")
    add_code(doc, "浏览器 Vue SPA\n    ↓ HTTP JSON / Session Cookie / CSRF\nSpring MVC Controller → Spring Security / CurrentUser\n    ↓\n业务 Service → MyBatis-Plus Mapper → MySQL 8\n                                    ↑\n                                 Flyway")
    add_heading(doc, "3.3 功能模块结构", level=2)
    add_table(
        doc,
        ["模块", "后端包", "前端页面"],
        [
            ("认证与资料", "auth、profile、common/security", "login、register、profile"),
            ("家庭与成员", "household", "onboarding、members"),
            ("收支分类", "category", "categories"),
            ("收支流水", "ledger", "entries、EntryForm"),
            ("统计仪表盘", "statistics", "dashboard、TrendChart"),
            ("错误与导航", "common/error", "router、forbidden、not-found"),
        ],
        [Inches(1.3), Inches(2.2), Inches(2.4)],
    )
    add_heading(doc, "3.4 数据库设计", level=2)
    add_table(
        doc,
        ["表", "主要字段", "关键约束"],
        [
            ("household", "id、name、invite_code、created_by", "邀请码全局唯一"),
            ("app_user", "账号、密码摘要、家庭、编号、角色、状态", "用户名全局唯一；成员编号家庭内唯一"),
            ("finance_category", "家庭、范围、类型、名称、状态", "系统/家庭范围一致；同类型名称唯一"),
            ("ledger_entry", "家庭、成员、分类、类型、金额、日期、删除标记", "金额大于零；外键；家庭/日期索引"),
        ],
        [Inches(1.25), Inches(2.7), Inches(1.95)],
    )
    add_heading(doc, "3.5 权限、安全与社会责任", level=2)
    add_body(doc, "前端操作显隐只负责用户体验，后端服务层才是安全边界。每次访问成员、分类或流水都要验证对象属于当前家庭。密码只保存摘要，错误响应不返回堆栈、数据库密码、Cookie 或 Token。系统不连接银行、支付平台和真实投资账户，避免把课程系统误解为金融建议或真实资金系统。")


def add_leader_detail(doc: Document) -> None:
    add_heading(doc, "四、详细设计", level=1, page_break=True)
    add_heading(doc, "4.1 代码组织与负责文件", level=2)
    add_table(doc, ["范围", "代表文件"], [
        ("安全", "common/security/SecurityConfig.java、CurrentUserService.java、InactiveSessionFilter.java"),
        ("认证", "auth/web/AuthController.java、auth/service/AuthService.java、UserDetailsServiceImpl.java"),
        ("家庭", "household/service/HouseholdService.java、household/web/HouseholdController.java"),
        ("资料", "profile/service/ProfileService.java、profile/web/ProfileController.java"),
        ("数据库", "resources/db/migration/V1__create_schema.sql、application*.yml"),
        ("集成", "README.md、docs/、DemoDataInitializer.java"),
    ], [Inches(1.2), Inches(4.7)])
    add_heading(doc, "4.2 安全链与会话", level=2)
    add_body(doc, "SecurityConfig 禁用默认表单登录和 HTTP Basic，使用 DaoAuthenticationProvider 与 BCrypt。登录成功后保存服务端 Session 并变更 Session ID；CookieCsrfTokenRepository 给浏览器提供 CSRF Token，所有写接口必须携带对应请求头。认证入口返回 401，权限拒绝返回 403 JSON。")
    add_code(doc, "CookieCsrfTokenRepository csrfRepository =\n    CookieCsrfTokenRepository.withHttpOnlyFalse();\nhttp.csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))\n    .sessionManagement(session -> session\n        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)\n        .sessionFixation(fixation -> fixation.changeSessionId()));")
    add_heading(doc, "4.3 当前用户与家庭边界", level=2)
    add_body(doc, "CurrentUserService 分为 requireUser、requireHouseholdUser 和 requireParent 三层。它从 SecurityContext 获取用户名后再次查库，确认账号仍为 ACTIVE，并返回只读 CurrentUser。各业务 Service 使用该对象的 householdId，而不读取客户端家庭参数。")
    add_heading(doc, "4.4 创建与加入家庭", level=2)
    add_body(doc, "创建家庭在单事务中插入家庭并把创建者设为 M001/PARENT。加入家庭把邀请码转为大写，定位并锁定家庭，复核邀请码仍有效，锁定成员序号后生成下一个编号，最后更新用户家庭与角色。邀请码使用排除易混淆字符的安全随机字符集，数据库唯一索引提供最终冲突保护。")
    add_code(doc, "Household household = householdMapper.selectForUpdateById(found.getId());\nint sequence = userMapper.maxMemberSequenceForUpdate(household.getId()) + 1;\nString memberNo = \"M\" + String.format(\"%03d\", sequence);\nuser.setHouseholdId(household.getId());\nuser.setMemberNo(memberNo);\nuser.setRole(Role.MEMBER);")
    add_heading(doc, "4.5 数据库与错误模型", level=2)
    add_body(doc, "Flyway V1 迁移建立四表、外键、检查约束和组合索引。业务异常使用稳定 code、中文 message 和可选 fieldErrors，Controller 统一返回真实 HTTP 状态。数据库约束负责最终一致性，服务层负责给用户可理解的错误。")
    add_heading(doc, "4.6 测试与集成", level=2)
    add_body(doc, "认证测试覆盖注册冲突和密码摘要；迁移测试验证空库建表；最终集成验证 Session、CSRF、家庭创建/加入、停用账号旧会话失效、邀请码并发保护和跨家庭隔离。组长还负责对齐前后端 DTO，执行 Maven、前端类型检查、单元测试、生产构建和真实浏览器流程。")


def add_backend_detail(doc: Document) -> None:
    add_heading(doc, "四、详细设计", level=1, page_break=True)
    add_heading(doc, "4.1 代码组织与负责文件", level=2)
    add_table(doc, ["模块", "Controller / Service / DTO / Test"], [
        ("成员", "MemberController、MemberService、MemberResponse、MemberServiceTest"),
        ("分类", "CategoryController、CategoryService、CategoryMapper、category/dto"),
        ("收支", "LedgerController、LedgerService、LedgerEntry、EntryQuery、LedgerServiceTest"),
        ("统计", "StatisticsController、StatisticsService、DashboardResponse"),
    ], [Inches(1.2), Inches(4.7)])
    add_heading(doc, "4.2 成员管理", level=2)
    add_body(doc, "成员列表按当前家庭查询。修改接口要求当前用户为家长，并通过 targetInHousehold 同时检查成员 ID 与家庭 ID。若操作会使活跃家长数量变为零，服务拒绝停用或降级；成员编号变化时检查家庭内唯一。")
    add_code(doc, "CurrentUser parent = currentUserService.requireParent();\nAppUser target = targetInHousehold(id, parent.householdId());\nif (target.getRole() == Role.PARENT && request.role() != Role.PARENT\n        && userMapper.countActiveParents(parent.householdId()) <= 1) {\n    throw new ApiException(HttpStatus.CONFLICT,\n        \"LAST_PARENT_REQUIRED\", \"家庭至少需要一名家长\");\n}")
    add_heading(doc, "4.3 分类管理", level=2)
    add_body(doc, "分类列表把系统分类与当前家庭分类合并。创建、改名和状态修改仅家长可用；服务拒绝修改 SYSTEM 范围分类，校验分类类型、名称冲突和状态。停用只改变状态，因此历史流水仍能关联并展示原分类。")
    add_heading(doc, "4.4 收支管理", level=2)
    add_body(doc, "EntryQuery 统一分页参数。普通成员查询时成员条件被强制为本人；家长才允许选择本家庭其他成员。新增/修改依次校验目标成员、分类可见性与启用状态、类型匹配、金额和日期，再保存清理后的备注。删除把 deleted 设为 true。")
    add_code(doc, "AppUser member = resolveMember(request.memberId(), user, true);\nFinanceCategory category = categoryService.requireUsable(\n    request.categoryId(), user.householdId(), request.type());\nvalidateDate(request.occurredOn());\nentry.setAmount(request.amount());\nentry.setNote(cleanNote(request.note()));\nentryMapper.insert(entry);")
    add_heading(doc, "4.5 统计服务", level=2)
    add_body(doc, "StatisticsService 按当前家庭和时间范围读取未删除流水，使用 BigDecimal 汇总收入、支出与结余。12 月趋势先创建完整月份序列，再把实际数据填入；类别构成按类型和分类分组；成员对比为每人分别累计收入、支出和结余；最近流水按统一倒序规则返回。")
    add_heading(doc, "4.6 测试设计", level=2)
    add_body(doc, "测试重点覆盖普通成员只能操作本人、家长可操作全家、跨家庭对象拒绝、最后家长保护、未来日期和类型不匹配、逻辑删除后列表与统计排除、金额和统计精度。测试使用专用 MySQL 环境，避免修改本机已有数据库。")


def add_frontend_detail(doc: Document) -> None:
    add_heading(doc, "四、详细设计", level=1, page_break=True)
    add_heading(doc, "4.1 代码组织与负责文件", level=2)
    add_table(doc, ["范围", "代表文件"], [
        ("基础", "main.ts、App.vue、router/index.ts、stores/auth.ts、api/client.ts"),
        ("布局组件", "layouts/AppLayout.vue、AuthLayout.vue、components/*"),
        ("业务页面", "views/DashboardView.vue、EntriesView.vue、MembersView.vue、CategoriesView.vue"),
        ("认证与状态", "LoginView.vue、RegisterView.vue、OnboardingView.vue、ForbiddenView.vue、NotFoundView.vue"),
        ("样式测试", "styles/tokens.css、main.css、src/__tests__/*"),
    ], [Inches(1.2), Inches(4.7)])
    add_heading(doc, "4.2 会话恢复与路由守卫", level=2)
    add_body(doc, "auth store 保存当前用户和恢复状态。进入受保护路由前先调用 `/api/auth/me`；未登录跳转登录，已登录未入户跳转 onboarding，已入户进入业务页。Axios 收到 401 时清理会话，收到真实 403 时发布事件进入专用页面，404 由前端兜底路由处理。")
    add_code(doc, "if (status === 401) {\n  window.dispatchEvent(new CustomEvent('auth:unauthorized'))\n}\nif (status === 403) {\n  window.dispatchEvent(new CustomEvent('auth:forbidden'))\n}\n// App.vue 分别导航到 /login 或 /forbidden")
    add_heading(doc, "4.3 仪表盘数据映射", level=2)
    add_body(doc, "DashboardView 维护本月、近 3 个月、今年和自定义时间范围，计算起止日期后调用统计接口。totals 映射三张金额卡片，trend 映射 ECharts 折线/面积图，categoryBreakdown 按收入支出切换，memberComparisons 显示每人收入、支出与结余。")
    add_code(doc, "const loadDashboard = async () => {\n  loading.value = true\n  try {\n    dashboard.value = await getDashboard(activeRange.value)\n  } finally {\n    loading.value = false\n  }\n}")
    add_heading(doc, "4.4 表单与业务页面", level=2)
    add_body(doc, "EntryForm 复用新增和编辑字段，流水类型变化时过滤对应分类。EntriesView 维护筛选和分页；MembersView 按角色决定是否显示邀请码、复制、重置和编辑按钮；CategoriesView 区分系统与家庭分类；ProfileView 修改密码成功后清空表单、退出并带提示回到登录页。")
    add_heading(doc, "4.5 响应式与视觉", level=2)
    add_body(doc, "应用壳使用浅灰背景、白色大圆角画布和胶囊导航。品牌蓝、深色、收入绿、支出红通过 CSS token 统一。桌面流水使用表格，手机隐藏表格并使用卡片；时间范围、家庭通行证和成员金额区允许换行，390px 下页面滚动宽度与视口一致。")
    add_heading(doc, "4.6 前端测试", level=2)
    add_body(doc, "Vitest 与 Vue Test Utils 覆盖认证恢复、路由守卫、Axios 401/403、角色操作显隐、收支表单联动、仪表盘范围、邀请码复制、修改密码退出和 403 页面。最终还执行 TypeScript 类型检查和 Vite 生产构建，并在 1440px 与 390px 浏览器视口验收。")


def add_chapter_five(doc: Document, report: dict) -> None:
    add_heading(doc, "五、设计结果", level=1, page_break=True)
    add_body(doc, "以下界面来自当前本机运行的最终系统，使用演示家庭数据获取；截图不含密码、Cookie、Token 或私人浏览器信息。")
    for index, (filename, caption) in enumerate(report["screens"]):
        if index and index % 2 == 0:
            doc.add_page_break()
        add_picture(doc, filename, caption)
    add_heading(doc, "5.1 结果说明", level=2)
    if report["detail"] == "leader":
        add_body(doc, "认证页提供完整登录入口；家庭成员页把家庭名称、当前角色、邀请码和成员维护集中到同一页面；资料页区分基本资料与密码修改。上述界面分别对应认证、家庭和资料后端闭环。")
    elif report["detail"] == "backend":
        add_body(doc, "收支、成员、分类和仪表盘页面的数据均来自本人负责的后端业务接口。界面上的角色操作、历史数据保留、筛选和统计结果依赖服务层权限与规则校验。")
    else:
        add_body(doc, "认证、仪表盘、收支和家庭成员页面使用同一应用壳、导航和状态规范，信息层级清晰；主要动作位于稳定位置，适合桌面演示，并已完成手机布局验收。")


def build_report(filename: str, report: dict) -> Path:
    doc = Document(str(TEMPLATE))
    build_cover(doc, report)
    remove_body_after_cover(doc)
    section = doc.sections[0]
    section.start_type = WD_SECTION.NEW_PAGE
    footer = section.footer.paragraphs[0]
    clear_paragraph(footer)
    add_page_number(footer)

    doc.add_page_break()
    add_heading(doc, "报告填写说明", level=1)
    add_body(doc, "本报告依据学校《软件项目设计报告2026年》模板和当前家庭理财系统源码生成。姓名、学号、专业班级和指导教师为占位符，提交前必须替换；个人实现章节按小组责任范围组织，成员应根据真实接手、修改和测试记录确认最终表述。", first_indent=False)
    add_body(doc, f"本报告对应角色：{report['role']}；责任范围：{report['focus']}。", first_indent=False)

    add_common_chapter_one(doc, report)
    add_common_chapter_two(doc, report)
    add_common_chapter_three(doc)
    if report["detail"] == "leader":
        add_leader_detail(doc)
    elif report["detail"] == "backend":
        add_backend_detail(doc)
    else:
        add_frontend_detail(doc)
    add_chapter_five(doc, report)
    add_heading(doc, "设计总结", level=1, page_break=True)
    add_body(doc, report["summary"])
    add_heading(doc, "存在问题与改进方向", level=2)
    add_body(doc, "当前系统定位为本机课程设计，不包含多家庭、预算、通知、附件、导入导出、银行同步和公网部署。前端生产构建存在 ECharts/Element Plus 合并包体积 warning，但不影响演示。后续若课程要求变化，应先更新需求文档和验收标准，再决定是否扩展；不应为展示复杂度而提前引入微服务、缓存或消息队列。")

    output = OUTPUT_DIR / filename
    output.parent.mkdir(parents=True, exist_ok=True)
    doc.core_properties.title = "家庭理财系统的设计与实现"
    doc.core_properties.subject = f"{report['role']}个人软件项目设计报告"
    doc.core_properties.author = report["name"]
    doc.core_properties.keywords = "家庭理财系统, Spring Boot, Vue, 课程设计"
    doc.save(str(output))
    return output


def main() -> None:
    if not TEMPLATE.exists():
        raise FileNotFoundError(TEMPLATE)
    missing = [path for report in REPORTS.values() for path, _ in report["screens"] if not (SCREEN_DIR / path).exists()]
    if missing:
        raise FileNotFoundError(f"缺少报告截图：{sorted(set(missing))}")
    outputs = [build_report(filename, report) for filename, report in REPORTS.items()]
    for output in outputs:
        print(output)


if __name__ == "__main__":
    main()
