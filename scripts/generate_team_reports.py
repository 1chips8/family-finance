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
            ("数据库与配置", "核对八张业务表、V1/V2 迁移、约束与 demo Profile"),
            ("安全认证", "实现 Session、CSRF、BCrypt、停用会话过滤和统一 401/403"),
            ("家庭与资料", "实现注册登录、创建/加入家庭、邀请码、姓名与密码修改"),
            ("集成交付", "对齐前后端 DTO，组织自动化门禁、浏览器验收和 README"),
        ],
        "detail": "leader",
        "screens": [
            ("01-login.png", "图 5-1 登录页：未输入账号密码的真实系统界面"),
            ("04-members.png", "图 5-2 家庭成员与家庭通行证：家长可查看邀请码和维护成员"),
            ("06-profile.png", "图 5-3 个人资料与修改密码：密码更新后要求重新登录"),
            ("09-audit.png", "图 5-4 操作日志：家长查看家庭业务变更记录"),
        ],
        "summary": (
            "本报告围绕需求落地、数据库、安全认证、家庭入户与集成验证展开。"
            "最需要谨慎处理的是身份与家庭边界：后端不能相信前端提交的家庭标识，所有业务必须从 Session 当前用户推导家庭范围；"
            "邀请码加入还需要在事务中锁定家庭和成员编号序列。通过统一异常模型、数据库约束和服务层校验，系统能够稳定区分 401、403、业务冲突和输入错误。"
            "新增预算、周期记账和审计后，数据库由四张扩展为八张业务表。后续重点是完善边界回归和下载文件验收；通知与公网部署仍不属于当前交付。"
        ),
    },
    "02-组员一-软件项目设计报告.docx": {
        "name": "[组员一姓名]",
        "student_id": "[组员一学号]",
        "role": "组员一",
        "focus": "成员、分类、收支、预算、周期记账、统计与审计后端",
        "personal_tasks": [
            ("成员管理", "成员列表、编号/姓名/角色/状态维护和最后家长保护"),
            ("分类管理", "系统分类只读、家庭分类新增/改名/停用/恢复"),
            ("收支管理", "分页筛选、增改删、普通成员本人范围和逻辑删除"),
            ("统计服务", "收入、支出、结余、趋势、构成、成员对比和最近流水"),
            ("扩展业务", "预算、周期模板与去重生成、CSV 预览导入、统计增强和审计"),
            ("后端测试", "验证权限、跨家庭隔离、校验规则、删除与统计精度"),
        ],
        "detail": "backend",
        "screens": [
            ("03-entries.png", "图 5-1 收支明细：支持类型、分类、成员和日期筛选"),
            ("04-members.png", "图 5-2 家庭成员：家长可编辑资料和停用成员"),
            ("05-categories.png", "图 5-3 收支分类：系统分类只读，家庭分类可维护"),
            ("02-dashboard.png", "图 5-4 月报页局部：本期金额与环比摘要"),
            ("07-budgets.png", "图 5-5 预算页局部：月份与家庭总预算设置"),
            ("08-recurring.png", "图 5-6 周期记账：按月生成并防止重复入账"),
        ],
        "summary": (
            "本报告覆盖成员、分类、流水、统计以及新增理财辅助模块。实现中的关键"
            "是让每一次对象访问同时满足当前家庭范围、当前角色权限和业务状态约束。流水保存采用 BigDecimal 和数据库 DECIMAL，"
            "删除采用逻辑删除，分类停用保留历史引用，统计统一排除已删除记录。CSV 导入先预览校验，再以事务提交；周期生成通过模板与月份唯一约束去重。最后家长保护和跨家庭对象拒绝是重要的失败路径。"
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
            ("业务页面", "仪表盘、流水、预算、周期记账、日志、成员、分类与资料"),
            ("响应式", "桌面表格、手机卡片、控件换行和无横向溢出"),
            ("前端测试", "路由守卫、角色显隐、表单联动、错误分流和构建门禁"),
        ],
        "detail": "frontend",
        "screens": [
            ("01-login.png", "图 5-1 登录页：蓝色纸感插画背景，移除左上角商标"),
            ("02-dashboard.png", "图 5-2 月报页局部：时间范围、本期金额和环比"),
            ("03-entries.png", "图 5-3 收支明细：筛选栏、表格和记一笔入口"),
            ("04-members.png", "图 5-4 家庭成员：家庭通行证与角色化操作"),
            ("07-budgets.png", "图 5-5 预算页局部：总预算未设置时的真实状态"),
            ("08-recurring.png", "图 5-6 周期记账页面：模板管理与按月生成入口"),
        ],
        "summary": (
            "本报告说明后端接口如何组织为 Vue 用户动线。前端通过 Pinia 恢复会话，通过路由守卫区分未登录、未入户和已入户状态，"
            "通过 Axios 拦截器转发 403 事件。仪表盘把统计 DTO 映射为金额卡片、趋势图、构成、成员对比和月度报告；"
            "流水页面在桌面使用表格，在手机使用卡片，保持主要操作一致。测试重点覆盖角色显隐、表单联动、密码更新退出和错误页面。"
            "路由、Element Plus 与 ECharts 已采用懒加载或按需引入；登录页在确定的蓝色与朱红配色上加入纸张、账本和住宅插画，并去掉左上角商标。后续应继续验证不同浏览器下载结果和辅助技术体验。"
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
    paragraph = doc.add_paragraph(style=f"Heading {level}")
    # The school template fixes chapter order, not a separate page per chapter.
    # Keep the cover separate; let chapters and figure pairs flow naturally.
    paragraph.paragraph_format.page_break_before = page_break and text.startswith("一、")
    paragraph.paragraph_format.keep_with_next = True
    paragraph.paragraph_format.space_before = Pt(10 if level == 1 else 7)
    paragraph.paragraph_format.space_after = Pt(6)
    run = paragraph.add_run(text)
    set_east_asia_font(run, "黑体")
    run.bold = True
    run.font.color.rgb = RGBColor(0, 0, 0)
    run.font.size = Pt(14 if level == 1 else 12)


def add_bullets(doc: Document, items: list[str]) -> None:
    for item in items:
        paragraph = doc.add_paragraph(style=None)
        paragraph.paragraph_format.left_indent = Pt(22)
        paragraph.paragraph_format.first_line_indent = Pt(-11)
        paragraph.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
        paragraph.paragraph_format.space_after = Pt(3)
        run = paragraph.add_run("• " + item)
        set_east_asia_font(run, "宋体")
        run.font.size = Pt(11)


def add_table(doc: Document, headers: list[str], rows: list[tuple[str, ...]], widths=None) -> None:
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    table.autofit = False
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        border = OxmlElement(f"w:{edge}")
        for name, value in (("val", "single"), ("sz", "4"), ("color", "D9D9D9")):
            border.set(qn(f"w:{name}"), value)
        borders.append(border)
    table._tbl.tblPr.append(borders)
    margins = OxmlElement("w:tblCellMar")
    for edge, size in (("top", "60"), ("bottom", "60"), ("left", "80"), ("right", "80")):
        margin = OxmlElement(f"w:{edge}")
        margin.set(qn("w:w"), size)
        margin.set(qn("w:type"), "dxa")
        margins.append(margin)
    table._tbl.tblPr.append(margins)
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
    for row in table.rows:
        for cell in row.cells:
            for paragraph in cell.paragraphs:
                paragraph.paragraph_format.line_spacing = 1.15
                paragraph.paragraph_format.space_before = Pt(0)
                paragraph.paragraph_format.space_after = Pt(0)
                snap = OxmlElement("w:snapToGrid")
                snap.set(qn("w:val"), "0")
                paragraph._p.get_or_add_pPr().append(snap)
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
    picture = paragraph.add_run().add_picture(str(path), width=Inches(5.9))
    picture._inline.docPr.set("descr", caption)
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
            ("工程与数据库", "前后端骨架、八表迁移、配置样例", "V1/V2 Flyway SQL", "完成"),
            ("认证与权限", "Session、CSRF、家庭入户、角色边界", "认证与家庭闭环", "完成"),
            ("核心业务", "成员、分类、流水和资料", "业务 CRUD 与校验", "完成"),
            ("统计与前端", "仪表盘、页面壳、响应式和状态页", "可演示 Web 系统", "完成"),
            ("理财扩展", "预算、周期记账、CSV、审计", "新增服务和业务页面", "完成"),
            ("测试与报告", "门禁证据、真实截图、报告同步", "当前版本报告材料", "待个人确认"),
        ],
        [Inches(1.05), Inches(2.05), Inches(1.85), Inches(0.9)],
    )
    add_heading(doc, "1.2 模块责任范围", level=2)
    add_body(doc, f"{report['role']}报告的模块范围为{report['focus']}。下表为责任划分，提交前应根据实际参与记录确认个人贡献。")
    add_table(doc, ["任务", "工作内容"], report["personal_tasks"], [Inches(1.35), Inches(4.55)])
    add_heading(doc, "1.3 可行性分析", level=2)
    add_body(doc, "技术可行性：Java 17、Spring Boot、Spring Security、MyBatis-Plus、Vue 3、Element Plus 和 ECharts 均有成熟工具链，前后端通过 JSON 接口解耦，适合课程设计规模。")
    add_body(doc, "经济可行性：项目使用开源框架和本机 MySQL，不需要云服务、域名、短信、支付或第三方付费接口，主要成本为小组开发和测试时间。")
    add_body(doc, "运行可行性：系统采用浏览器访问，用户只需完成登录、创建或加入家庭即可使用；README 提供精确启动命令和演示账号。")
    add_body(doc, "进度可行性：模块按接口边界分配，后端业务和前端页面可在统一契约下并行；最终由组长执行一次集成门禁和浏览器验收。")
    add_heading(doc, "1.4 技术取舍", level=2)
    add_body(doc, "本项目选择前后端分离、组件化界面、服务端鉴权和自动化构建。核心功能覆盖家庭协作、权限隔离、流水与统计，扩展功能补充预算计划、批量数据交换和重复记账。仍采用单体后端与关系数据库，不接入真实金融账户，不引入没有当前需求的微服务和消息队列。")


def add_common_chapter_two(doc: Document, report: dict) -> None:
    add_heading(doc, "二、需求分析", level=1, page_break=True)
    add_heading(doc, "2.1 用户角色", level=2)
    add_table(
        doc,
        ["角色", "主要目标", "权限摘要"],
        [
            ("未入户用户", "完成家庭归属", "注册登录后创建家庭或输入邀请码加入"),
            ("普通成员", "维护个人收支并查看家庭整体", "只能维护本人流水，可查看成员名单和家庭聚合统计"),
            ("家长", "管理家庭和全家收支", "维护成员、分类、全家流水与预算，查看审计日志"),
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
        "预算：月度总预算与分类预算，展示已用、剩余、使用率和超支状态。",
        "数据交换：按权限导出 CSV；导入先预览字段和逐行错误，通过后整批提交。",
        "周期记账：维护重复收支模板，手动按月生成到期流水，防止同月重复生成。",
        "统计增强与审计：等长上期环比、储蓄率、异常提示、月度报告；家长查看操作日志。",
    ])
    add_heading(doc, "2.3 核心业务规则", level=2)
    add_bullets(doc, [
        "一个账号只属于一个家庭；创建者为家长，邀请码加入者为普通成员。",
        "所有家庭范围从当前 Session 用户取得，不接受客户端家庭 ID 作为可信来源。",
        "普通成员只能维护本人流水；最后一名活跃家长不能停用或降级。",
        "系统分类不可编辑；停用分类不能用于新流水，但历史数据保留。",
        "金额必须大于零并保留两位小数；类别类型必须与收入/支出类型一致；日期不晚于当天。",
        "流水逻辑删除后立即从列表和统计中排除。",
        "预算只允许家长修改，普通成员可以查看；CSV 和周期模板沿用家长全家、成员本人的数据边界。",
        "周期生成由用户触发，不是后台自动扣款；同一模板同一月份只生成一次。",
    ])
    add_heading(doc, "2.4 非功能需求", level=2)
    add_body(doc, "安全方面使用 BCrypt、服务端 Session、CSRF、统一鉴权和家庭隔离；可靠性方面使用事务、外键、唯一索引和可恢复状态；易用性方面提供加载、空数据、字段错误、403、404 和重试状态；兼容性方面以桌面 Chrome/Edge 为主并适配 390px 手机宽度。")
    add_heading(doc, "2.5 模块验收标准", level=2)
    add_body(doc, f"{report['focus']}必须与统一接口契约一致，既能完成正常动线，也应拒绝越权、无效输入和冲突操作。成员删除以停用替代物理删除，保留历史账目；收入与支出构成通过分类和流水共同维护。此业务解释及 Spring Boot 对任务书 JavaEE 技术要求的对应关系应在课程验收时确认。")


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
            ("预算与周期记账", "budget、recurring", "budgets、recurring"),
            ("CSV 与操作日志", "ledger、audit", "entries 导入导出、audit-logs"),
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
            ("monthly_budget", "家庭、月份、可空分类、金额", "家庭/月份/分类唯一；月份为月首"),
            ("recurring_template", "家庭、成员、分类、金额、每月日期、状态", "每月日期 1 至 31；金额大于零"),
            ("recurring_generation", "模板、生成月份、流水 ID", "模板/月份唯一，防止重复生成"),
            ("audit_log", "家庭、操作者、动作、对象、摘要、时间", "按家庭/时间查询；不记录密码"),
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
        ("数据库", "resources/db/migration/V1__create_schema.sql、V2 迁移、application*.yml"),
        ("集成", "README.md、docs/、DemoDataInitializer.java"),
    ], [Inches(1.2), Inches(4.7)])
    add_heading(doc, "4.2 安全链与会话", level=2)
    add_body(doc, "SecurityConfig 禁用默认表单登录和 HTTP Basic，使用 DaoAuthenticationProvider 与 BCrypt。登录后保存 SecurityContext；CookieCsrfTokenRepository 给浏览器提供 CSRF Token，写请求按安全链规则校验。未认证访问受保护接口返回 401，权限拒绝返回 403 JSON。会话固定攻击防护配置与自定义登录路径的集成效果需作为安全回归项验证。")
    add_code(doc, "CookieCsrfTokenRepository csrfRepository =\n    CookieCsrfTokenRepository.withHttpOnlyFalse();\nhttp.csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))\n    .sessionManagement(session -> session\n        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)\n        .sessionFixation(fixation -> fixation.changeSessionId()));")
    add_heading(doc, "4.3 当前用户与家庭边界", level=2)
    add_body(doc, "CurrentUserService 分为 requireUser、requireHouseholdUser 和 requireParent 三层。它从 SecurityContext 获取用户名后再次查库，确认账号仍为 ACTIVE，并返回只读 CurrentUser。各业务 Service 使用该对象的 householdId，而不读取客户端家庭参数。")
    add_heading(doc, "4.4 创建与加入家庭", level=2)
    add_body(doc, "创建家庭在单事务中插入家庭并把创建者设为 M001/PARENT。加入家庭把邀请码转为大写，定位并锁定家庭，复核邀请码仍有效，锁定成员序号后生成下一个编号，最后更新用户家庭与角色。邀请码使用排除易混淆字符的安全随机字符集，数据库唯一索引提供最终冲突保护。")
    add_body(doc, "实现位置：household/service/HouseholdService.java。加入家庭的事务处理应与家庭锁、成员编号唯一约束一起理解，不能只依赖前端禁用按钮保证并发安全。")
    add_heading(doc, "4.5 数据库与错误模型", level=2)
    add_body(doc, "Flyway V1 建立家庭、用户、分类、流水四表，V2 增加月度预算、周期模板、生成记录和审计日志四表。周期生成以模板与月份组合唯一，预算以家庭、月份和分类唯一。业务异常使用稳定 code、中文 message 和可选 fieldErrors，接口返回真实 HTTP 状态。")
    add_heading(doc, "4.6 测试与集成", level=2)
    add_body(doc, "已记录后端 clean verify、前端测试与生产构建证据，V1/V2 在隔离 MySQL 8.4 库验证通过。API 冒烟覆盖认证、业务查询、新增与清理、预算、周期模板和 CSV 链路。测试次数不等于所有场景覆盖；邀请码并发、会话固定防护和各浏览器文件落盘仍需按专项用例验证。")


def add_backend_detail(doc: Document) -> None:
    add_heading(doc, "四、详细设计", level=1, page_break=True)
    add_heading(doc, "4.1 代码组织与负责文件", level=2)
    add_table(doc, ["模块", "Controller / Service / DTO / Test"], [
        ("成员", "MemberController、MemberService、MemberResponse、MemberServiceTest"),
        ("分类", "CategoryController、CategoryService、CategoryMapper、category/dto"),
        ("收支", "LedgerController、LedgerService、LedgerEntry、EntryQuery、LedgerServiceTest"),
        ("统计", "StatisticsController、StatisticsService、DashboardResponse"),
        ("扩展", "BudgetService、RecurringService、EntryCsvService、AuditLogService"),
    ], [Inches(1.2), Inches(4.7)])
    add_heading(doc, "4.2 成员管理", level=2)
    add_body(doc, "成员列表按当前家庭查询。修改接口要求当前用户为家长，并通过 targetInHousehold 同时检查成员 ID 与家庭 ID。若操作会使活跃家长数量变为零，服务拒绝停用或降级；成员编号变化时检查家庭内唯一。")
    add_body(doc, "最后家长保护同时覆盖降级和停用，不能仅统计界面显示的成员数量。相关实现位于 MemberService，需结合服务事务和数据库锁检查并发行为。")
    add_heading(doc, "4.3 分类管理", level=2)
    add_body(doc, "分类列表把系统分类与当前家庭分类合并。创建、改名和状态修改仅家长可用；服务拒绝修改 SYSTEM 范围分类，校验分类类型、名称冲突和状态。停用只改变状态，因此历史流水仍能关联并展示原分类。")
    add_heading(doc, "4.4 收支管理", level=2)
    add_body(doc, "EntryQuery 统一分页参数。普通成员查询时成员条件被强制为本人；家长才允许选择本家庭其他成员。新增/修改依次校验目标成员、分类可见性与启用状态、类型匹配、金额和日期，再保存清理后的备注。删除把 deleted 设为 true。")
    add_body(doc, "CSV 出口按筛选条件与角色范围生成 UTF-8 数据；导入提供预览和逐行错误，正式提交重新校验并在事务中完成。单次成功 Toast 只表示前端发起下载，不能作为浏览器已保存文件的验收证据。")
    add_heading(doc, "4.5 统计服务", level=2)
    add_body(doc, "StatisticsService 按当前家庭和时间范围读取未删除流水，使用 BigDecimal 汇总收入、支出与结余。12 月趋势先创建完整月份序列，再把实际数据填入；类别构成按类型和分类分组；成员对比为每人分别累计收入、支出和结余；最近流水按统一倒序规则返回。")
    add_body(doc, "扩展统计将当前日期范围与紧邻的等长上期比较，提供储蓄率及支出异常提示；基期为零时不返回增长率。这里的环比不等于去年同期同比。预算按指定月份的支出计算使用率。周期模板只在手动触发生成时创建到期流水，月末日期按当月天数处理，唯一索引防止重复月份入账。")
    add_heading(doc, "4.6 测试设计", level=2)
    add_body(doc, "定向测试关注对象隔离、角色权限、成员保护、金额精度、统计比较、预算与周期生成等规则。已记录的 API 冒烟包含 CSV 导出、预览、导入及测试数据清理，普通成员导出隔离与预算、审计拒绝访问。完整用例以测试源码和 docs/stages/05 的验收记录为准，不将设计用例等同于已经执行。")


def add_frontend_detail(doc: Document) -> None:
    add_heading(doc, "四、详细设计", level=1, page_break=True)
    add_heading(doc, "4.1 代码组织与负责文件", level=2)
    add_table(doc, ["范围", "代表文件"], [
        ("基础", "main.ts、App.vue、router/index.ts、stores/auth.ts、api/client.ts"),
        ("布局组件", "layouts/AppLayout.vue、AuthLayout.vue、components/*"),
        ("业务页面", "views/DashboardView.vue、EntriesView.vue、MembersView.vue、CategoriesView.vue"),
        ("新增页面", "BudgetsView.vue、RecurringView.vue、AuditLogsView.vue"),
        ("认证与状态", "LoginView.vue、RegisterView.vue、OnboardingView.vue、ForbiddenView.vue、NotFoundView.vue"),
        ("样式测试", "styles/tokens.css、各模块样式、login-background.css、src/__tests__/*"),
    ], [Inches(1.2), Inches(4.7)])
    add_heading(doc, "4.2 会话恢复与路由守卫", level=2)
    add_body(doc, "auth store 保存当前用户和恢复状态。路由守卫在会话恢复后区分未登录、未入户和已入户；Axios 收到真实 403 时发布 family-finance:forbidden 事件，App.vue 监听后进入权限页面。404 由前端兜底路由处理。")
    add_body(doc, "实现位置：api/client.ts 的 FORBIDDEN_EVENT、App.vue 的事件监听、router/index.ts 的路由守卫。页面路由采用动态导入，避免登录时同步加载所有业务页面。")
    add_heading(doc, "4.3 仪表盘数据映射", level=2)
    add_body(doc, "DashboardView 维护本月、近 3 个月、今年和自定义时间范围，计算起止日期后调用统计接口。totals 映射三张金额卡片，trend 映射 ECharts 折线/面积图，composition 按收入支出切换，members 显示每人收入、支出与结余。")
    add_body(doc, "新增月度报告和统计摘要；TrendChart 模块化注册所需 ECharts 图表、组件与渲染器，并提供可读文本摘要。预算和环比信息需要清楚标明统计月份与比较基准，不能把等长上期环比解释为同比。")
    add_heading(doc, "4.4 表单与业务页面", level=2)
    add_body(doc, "EntryForm 复用新增和编辑字段，流水类型变化时过滤对应分类。EntriesView 维护筛选和分页；MembersView 按角色决定是否显示邀请码、复制、重置和编辑按钮；CategoriesView 区分系统与家庭分类；ProfileView 修改密码成功后清空表单、退出并带提示回到登录页。")
    add_body(doc, "流水页面监听 new 查询参数，支持同路由打开记账抽屉，关闭后清理参数；默认今天改为本地日期计算。CSV 导出采用同源下载地址，导入以预览、错误确认和提交组成闭环。预算和日志按角色控制操作入口，后端仍独立校验权限。")
    add_heading(doc, "4.5 响应式与视觉", level=2)
    add_body(doc, "风格 C 采用蓝色、朱红与纸白，借助清晰的分栏、细线和账页感建立信息层级。登录页新增蓝色纸张、账本、铅笔和住宅插画，正文与输入卡片保持对比度，左上角商标仅在登录页隐藏。背景资源为本地 PNG，不依赖第三方在线图片。桌面流水用表格，手机用卡片；移动端背景增加遮罩保证文字可读。")
    add_heading(doc, "4.6 前端测试", level=2)
    add_body(doc, "当前已记录 25 个前端测试通过，类型检查与 Vite 生产构建通过；登录页背景与商标修改后再次通过生产构建，并检查桌面与窄屏效果。本次材料同步重新采集真实页面截图，不把截图采集等同于全量功能或所有设备验证。")


def add_chapter_five(doc: Document, report: dict) -> None:
    add_heading(doc, "五、设计结果", level=1, page_break=True)
    add_body(doc, "以下为 2026 年 9 月 6 日从本地运行系统采集的真实视口截图，长页面仅展示局部。登录截图清空凭据，业务截图中的成员及流水使用演示家庭数据。")
    for filename, caption in report["screens"]:
        add_picture(doc, filename, caption)
    add_heading(doc, "5.1 结果说明", level=2)
    if report["detail"] == "leader":
        add_body(doc, "认证页提供完整登录入口；家庭成员页把家庭名称、当前角色、邀请码和成员维护集中到同一页面；资料页区分基本资料与密码修改。上述界面分别对应认证、家庭和资料后端闭环。")
    elif report["detail"] == "backend":
        add_body(doc, "收支、成员、分类、仪表盘、预算和周期页面的数据来自对应后端业务接口。角色操作、历史数据保留、筛选和统计结果依赖服务层权限与规则校验。空状态如实表示演示环境尚未配置该数据，并非模拟成功记录。")
    else:
        add_body(doc, "业务页面使用统一应用壳、导航和状态规范；认证页面采用独立纸感背景。已有窄屏布局检查，本节截图展示桌面入口，不代替完整移动端交互验收。")


def build_report(filename: str, report: dict) -> Path:
    doc = Document(str(TEMPLATE))
    build_cover(doc, report)
    remove_body_after_cover(doc)
    section = doc.sections[0]
    section.start_type = WD_SECTION.NEW_PAGE
    footer = section.footer.paragraphs[0]
    clear_paragraph(footer)
    add_page_number(footer)

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
    add_body(doc, "当前系统已经包含预算、CSV 导入导出、周期记账、统计增强与审计；仍不包含多家庭切换、附件、通知、银行同步和公网部署。浏览器下载文件落盘与内容核对尚缺独立留证，不能仅凭 Toast 声称下载完成。后续优先补足边界回归、不同浏览器兼容性和辅助技术检查，不将课程系统宣传为真实资金管理或投资建议工具。")
    add_heading(doc, "版本与验证依据", level=2)
    add_body(doc, "材料更新日期：2026 年 9 月 6 日。功能扩展对应提交 6ac2e4b，CSV 前端调整对应 179f4ac，另包含当前工作区登录页视觉更新。此前后端 clean verify 为 18 个测试、0 失败，前端为 25 个测试并通过类型检查与构建；完整命令、历史记录及剩余验收项见 docs/stages/05-测试验收与报告素材.md。上述结果不是本次文档更新重新运行的全量测试。")

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
