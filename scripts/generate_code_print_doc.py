from __future__ import annotations

from pathlib import Path
from typing import Iterable

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "交付" / "家庭理财系统代码打印稿.docx"


FILES: list[tuple[str, str, str]] = [
    ("后端 / 安全配置", "backend/src/main/java/com/family/finance/common/security/SecurityConfig.java", "Java"),
    ("后端 / 收支服务", "backend/src/main/java/com/family/finance/ledger/service/LedgerService.java", "Java"),
    ("后端 / CSV 导入导出", "backend/src/main/java/com/family/finance/ledger/service/EntryCsvService.java", "Java"),
    ("后端 / 预算服务", "backend/src/main/java/com/family/finance/budget/service/BudgetService.java", "Java"),
    ("后端 / 周期流水服务", "backend/src/main/java/com/family/finance/recurring/service/RecurringService.java", "Java"),
    ("后端 / 统计服务", "backend/src/main/java/com/family/finance/statistics/service/StatisticsService.java", "Java"),
    ("后端 / 收支接口", "backend/src/main/java/com/family/finance/ledger/web/LedgerController.java", "Java"),
    ("前端 / HTTP 客户端", "frontend/src/api/client.ts", "TypeScript"),
    ("前端 / 月度仪表盘", "frontend/src/views/DashboardView.vue", "Vue"),
    ("前端 / 收支明细", "frontend/src/views/EntriesView.vue", "Vue"),
    ("前端 / 登录布局", "frontend/src/layouts/AuthLayout.vue", "Vue"),
    ("前端 / 登录背景样式", "frontend/src/styles/login-background.css", "CSS"),
]


NOTES: dict[str, list[tuple[str, str]]] = {
    "SecurityConfig.java": [
        ("before: http", "注释：统一配置会话认证、CSRF、防未登录和越权响应；业务接口默认要求已认证。"),
        ("before: .sessionManagement", "注释：登录后保持服务端 Session，并在会话固定攻击防护中更换 Session ID。"),
    ],
    "LedgerService.java": [
        ("before: public EntryPageResponse list", "注释：列表查询先按家庭隔离；普通成员只能看到本人流水，家长可按成员筛选。"),
        ("before: public EntryResponse createImported", "注释：导入和手工新增共用此入口，集中执行成员、分类、日期和金额等业务校验。"),
        ("before: private LedgerEntry target", "注释：更新和删除再次检查家庭归属与角色范围，不能只依赖前端按钮显隐。"),
    ],
    "EntryCsvService.java": [
        ("before: public EntryImportCommitResponse commit", "注释：提交时重新解析并比对校验和，防止预览后 CSV 被替换；有效行在事务中批量写入。"),
        ("before: public byte[] export", "注释：导出复用带权限范围的流水查询，生成带 UTF-8 BOM 的 CSV，便于表格软件打开。"),
        ("before: private EntryImportPreviewResponse parseAndValidate", "注释：预览阶段检查表头、行数、成员、分类、日期和金额，并返回逐行错误。"),
    ],
    "BudgetService.java": [
        ("before: public", "注释：预算服务只接受家长维护，统计读取时再计算已用金额、使用率和超支状态。"),
    ],
    "RecurringService.java": [
        ("before: public", "注释：周期流水采用手动按月生成；模板月份唯一，月底日期会按当月最后一天收敛。"),
    ],
    "StatisticsService.java": [
        ("before: public", "注释：统计接口输出总额、趋势、分类构成、成员对比、环比、储蓄率、预算和异常提示。"),
    ],
    "LedgerController.java": [
        ("before: @RestController", "注释：控制器只负责 HTTP 参数和响应包装，权限与业务规则继续由服务层执行。"),
    ],
    "client.ts": [
        ("before: const client", "注释：Axios 实例统一携带 Cookie 和 CSRF 头，并把后端错误转换为页面可读消息。"),
        ("before: client.interceptors", "注释：401 回登录页，403 发布禁止事件；具体页面根据事件展示相应状态。"),
    ],
    "DashboardView.vue": [
        ("before: async function load", "// 注释：按当前范围加载月报，前端只负责选择范围和展示，金额计算由后端完成。"),
        ("before: function comparisonRate", "// 注释：这里展示等长上期环比，不把它误写成去年同期同比。"),
    ],
    "EntriesView.vue": [
        ("before: async function load", "// 注释：加载流水、成员和分类选项；普通成员的范围由服务端再次限制。"),
        ("before: async function exportCsv", "// 注释：导出链接沿用当前筛选条件；下载文件落盘仍应在浏览器验收中单独核对。"),
        ("before: watch(() => route.query.new", "// 注释：监听 query 以支持同一路由重复打开“记一笔”，关闭后清理参数。"),
    ],
    "AuthLayout.vue": [
        ("before: <template>", "<!-- 注释：登录页使用纸感插画背景；其他认证页面保留品牌入口。 -->"),
    ],
    "login-background.css": [
        ("before: .auth-layout--illustrated", "/* 注释：背景素材只承担装饰，表单控件仍使用真实 HTML 交互。 */"),
    ],
}


def set_cell_shading(cell, fill: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tc_pr.append(shd)


def set_cell_margins(cell, top=80, start=100, bottom=80, end=100) -> None:
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for side, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{side}"))
        if node is None:
            node = OxmlElement(f"w:{side}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def add_page_number(paragraph) -> None:
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = paragraph.add_run("第 ")
    fld = OxmlElement("w:fldSimple")
    fld.set(qn("w:instr"), "PAGE")
    paragraph._p.append(fld)
    paragraph.add_run(" 页")


def add_code_paragraph(doc: Document, text: str, line_no: int, is_comment: bool = False) -> None:
    paragraph = doc.add_paragraph(style="Code")
    paragraph.paragraph_format.keep_together = True
    prefix = f"{line_no:04d}  "
    run = paragraph.add_run(prefix + text)
    run.font.name = "Consolas"
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "等线")
    run.font.size = Pt(8.2)
    if is_comment:
        run.font.color.rgb = RGBColor(0x2E, 0x7D, 0x32)


def annotate(path: Path, language: str) -> list[tuple[str, bool]]:
    lines = path.read_text(encoding="utf-8").splitlines()
    rules = NOTES.get(path.name, [])
    result: list[tuple[str, bool]] = []
    used: set[int] = set()
    for idx, line in enumerate(lines):
        stripped = line.strip()
        for rule_index, (needle, note) in enumerate(rules):
            if rule_index in used:
                continue
            if needle.startswith("before: ") and stripped.startswith(needle[8:]):
                result.append((note, True))
                used.add(rule_index)
        result.append((line, stripped.startswith("//") or stripped.startswith("/*") or stripped.startswith("<!--")))
    return result


def configure_document(doc: Document) -> None:
    section = doc.sections[0]
    section.top_margin = Cm(1.55)
    section.bottom_margin = Cm(1.45)
    section.left_margin = Cm(1.55)
    section.right_margin = Cm(1.55)
    normal = doc.styles["Normal"]
    normal.font.name = "宋体"
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
    normal.font.size = Pt(10.5)
    code = doc.styles.add_style("Code", WD_STYLE_TYPE.PARAGRAPH)
    code.font.name = "Consolas"
    code._element.rPr.rFonts.set(qn("w:eastAsia"), "等线")
    code.font.size = Pt(8.2)
    code.paragraph_format.space_after = Pt(0)
    code.paragraph_format.line_spacing = 1.0
    for style_name, size, color in (("Title", 22, "1F4C96"), ("Heading 1", 15, "1F4C96"), ("Heading 2", 12, "BE3E2F")):
        style = doc.styles[style_name]
        style.font.name = "黑体"
        style._element.rPr.rFonts.set(qn("w:eastAsia"), "黑体")
        style.font.size = Pt(size)
        style.font.color.rgb = RGBColor.from_string(color)
    footer = section.footer.paragraphs[0]
    add_page_number(footer)


def add_intro(doc: Document) -> None:
    title = doc.add_paragraph(style="Title")
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title.add_run("家庭理财系统代码打印稿")
    subtitle = doc.add_paragraph()
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    subtitle.add_run("当前源码关键模块 · 带中文注释版 · 2026 年 9 月").bold = True
    doc.add_paragraph(
        "本打印稿从当前工作区源码整理，便于课程答辩、代码走查和纸质打印。代码保持与源码一致，"
        "仅在打印稿中于关键入口前增加解释性注释，不回写源文件。注释重点说明权限边界、事务、"
        "导入校验、统计口径和前端状态处理。"
    )
    table = doc.add_table(rows=1, cols=2)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    hdr = table.rows[0].cells
    hdr[0].text = "打印范围"
    hdr[1].text = "说明"
    for cell in hdr:
        set_cell_shading(cell, "DCE6F5")
        set_cell_margins(cell)
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
    for left, right in [
        ("后端核心", "安全配置、流水、CSV、预算、周期流水、统计与收支接口"),
        ("前端核心", "HTTP 客户端、月度仪表盘、收支明细、登录布局与背景样式"),
        ("阅读方式", "每个文件独立分页；左侧为行号，绿色文字为打印稿新增注释"),
    ]:
        cells = table.add_row().cells
        cells[0].text, cells[1].text = left, right
        for cell in cells:
            set_cell_margins(cell)
    doc.add_paragraph("源码路径均相对于项目根目录；打印日期和功能口径以当前工作区为准。")


def add_file(doc: Document, title: str, relative: str, language: str, index: int) -> None:
    if index > 0:
        doc.add_page_break()
    doc.add_heading(title, level=1)
    meta = doc.add_paragraph()
    meta.add_run(f"文件：{relative}\n语言：{language}    来源：当前工作区源码").italic = True
    path = ROOT / relative
    for line_no, (line, is_comment) in enumerate(annotate(path, language), start=1):
        add_code_paragraph(doc, line, line_no, is_comment)


def main() -> None:
    doc = Document()
    configure_document(doc)
    add_intro(doc)
    for index, (title, relative, language) in enumerate(FILES):
        add_file(doc, title, relative, language, index)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    doc.save(OUTPUT)
    print(OUTPUT)


if __name__ == "__main__":
    main()
