package silence.simsool.protector.obfuscator.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public final class I18n {

	public enum Lang { EN, KO }

	private static final Preferences PREFS = Preferences.userNodeForPackage(I18n.class);
	private static Lang currentLang = Lang.valueOf(PREFS.get("ui_lang", "EN"));
	private static final List<Runnable> listeners = new ArrayList<>();

	private I18n() {}

	public static Lang current() {
		return currentLang;
	}

	public static void setLang(Lang lang) {
		if (currentLang != lang) {
			currentLang = lang;
			PREFS.put("ui_lang", lang.name());
			for (Runnable listener : listeners) {
				listener.run();
			}
		}
	}

	public static void addListener(Runnable listener) {
		listeners.add(listener);
	}

	public static String get(String key) {
		boolean ko = currentLang == Lang.KO;
		return switch (key) {
			// Title & Slogans
			case "title" -> "Necron Obfuscator";
			case "slogan" -> "I like Meow with ArchTang.";
			case "quote" -> ko ? "\u201C새로운 관점의 보호\u201D" : "\u201CA New Perspective on Protection\u201D";
			case "quote_sub" -> ko ? "성능을 망가트리지 않고, 소스코드를 보호하세요." : "Protect your source code without sacrificing performance.";

			// Sidebar
			case "nav_project" -> ko ? "프로젝트" : "Project";
			case "nav_philosophy" -> ko ? "핵심 철학" : "Philosophy";
			case "nav_output" -> ko ? "출력 로그" : "Output Logs";
			case "active_config" -> ko ? "활성 설정" : "Active configuration";
			case "join_discord" -> ko ? "디스코드 참가" : "Join Discord";
			case "copyright" -> "\u00A9 2026 SILENCE";

			// Main Header
			case "header_title" -> ko ? "프로젝트" : "Project";
			case "header_desc" -> ko ? "입력 및 보호 옵션을 구성하고 견고한 출력물을 생성하세요." : "Configure your input, protection options and build the most resilient output.";

			// Cards
			case "card_io" -> ko ? "입출력 설정" : "Input / Output";
			case "card_io_sub" -> ko ? "입력 JAR 파일과 출력 경로를 지정하세요." : "Select your input JAR file and the output location.";
			case "input_jar" -> ko ? "입력 JAR" : "Input JAR";
			case "output_jar" -> ko ? "출력 JAR" : "Output JAR";
			case "browse" -> ko ? "찾아보기" : "Browse";

			case "card_main" -> ko ? "메인 클래스 / 패브릭" : "Main Class / Fabric";
			case "card_main_sub" -> ko ? "메인 클래스 및 패브릭 모드 설정을 지정하세요." : "Specify the main class and fabric configuration.";
			case "main_class" -> ko ? "메인 클래스" : "Main Class";
			case "fabric_toggle" -> ko ? "fabric.mod.json 진입점 자동 업데이트" : "Automatically update fabric.mod.json entrypoint";
			case "fabric_toggle_sub" -> ko ? "fabric.mod.json 파일 발견 시 진입점을 자동으로 변경합니다." : "If a fabric.mod.json file is found, the entrypoint will be updated automatically.";
			case "mixin_fixed_path" -> ko ? "믹스인 고정 경로 지정" : "Fixed Mixin Path";
			case "mixin_fixed_path_sub" -> ko ? "믹스인 클래스들을 지정된 고정 경로에 배치합니다." : "Place mixin classes under a designated fixed path.";

			case "card_opts" -> ko ? "보호 옵션" : "Protection Options";
			case "card_opts_sub" -> ko ? "적용할 난독화 기능을 활성화하세요. 가볍고 강력하게 설계되었습니다." : "Enable the protection features you want to use. Designed for strong, lightweight obfuscation.";
			case "opt_class" -> ko ? "클래스 이름" : "Class Names";
			case "opt_class_sub" -> ko ? "클래스 식별자 난독화" : "Obfuscate class names";
			case "opt_string" -> ko ? "문자열 암호화" : "String Encryption";
			case "opt_string_sub" -> ko ? "문자열 리터럴 암호화" : "Encrypt string literals";
			case "opt_method" -> ko ? "메서드 이름" : "Method Names";
			case "opt_method_sub" -> ko ? "메서드 식별자 난독화" : "Obfuscate method names";
			case "opt_number" -> ko ? "숫자 상수 암호화" : "Number Encryption";
			case "opt_number_sub" -> ko ? "정수/실수 상수 비트 변환" : "Encrypt numeric constants";
			case "opt_field" -> ko ? "필드 이름" : "Field Names";
			case "opt_field_sub" -> ko ? "필드 식별자 난독화" : "Obfuscate field names";
			case "opt_slogic" -> ko ? "무작위 SLogic" : "Randomized SLogic";
			case "opt_slogic_sub" -> ko ? "실행마다 무작위 복호화 제어흐름 생성" : "Insert randomized control flow";
			case "opt_package" -> ko ? "패키지 재배치" : "Randomize Packages";
			case "opt_package_sub" -> ko ? "패키지 구조 셔플 및 무작위화" : "Shuffle and randomize package structure";
			case "opt_anno" -> ko ? "어노테이션 보존" : "Preserve Annotated Methods";
			case "opt_anno_sub" -> ko ? "어노테이션이 붙은 메서드 보호 제외" : "Keep methods with annotations";

			case "card_pkg" -> ko ? "패키지 루트" : "Package Root";
			case "card_pkg_sub" -> ko ? "루트 패키지 옵션을 설정하세요." : "Configure package root options.";
			case "force_root" -> ko ? "패키지 루트 강제 지정" : "Force package root";
			case "force_root_sub" -> ko ? "지정된 루트 패키지로 모든 클래스를 재배치합니다." : "Repackage all classes into the specified root package.";

			case "card_rand" -> ko ? "무작위성 / 로직" : "Randomization / Logic";
			case "card_rand_sub" -> ko ? "무작위 시드 및 SLogic 템플릿 설정" : "Configure randomization settings and SLogic behavior.";
			case "seed_mode" -> ko ? "시드 모드" : "Seed mode";
			case "package_depth" -> ko ? "패키지 깊이" : "Package depth";
			case "slogic_template" -> ko ? "SLogic 템플릿" : "SLogic template";
			case "dynamic_info_title" -> ko ? "동적 런타임 로직" : "Dynamic Runtime Logic";
			case "dynamic_info_desc" -> ko ? "보호 로직이 매 빌드마다 무작위로 생성되고 재구성되어 매번 다른 바이트코드 패턴을 생성하여 분석 저항성을 극대화합니다." : "The protection logic is randomized and rebuilt on each build, creating different bytecode patterns every time for better resistance against analysis.";
			case "tip_seed_random" -> ko ? "SecureRandom 기반 매 빌드 고유 바이트코드 생성" : "Unique bytecode per build via SecureRandom.";
			case "tip_seed_fixed" -> ko ? "고정 시드 기반 결정론적 난독화 (재현/디버깅용)" : "Constant seed for deterministic reproducible builds.";
			case "tip_seed_timestamp" -> ko ? "빌드 시간 밀리초 기반 고유 버전 생성" : "Unique version generated per build timestamp.";
			case "tip_slogic_dynamic" -> ko ? "8개 정수/4개 Long 규칙 (권장 균형형)" : "8 int / 4 long rules with 2-4 op steps (Balanced).";
			case "tip_slogic_ultra" -> ko ? "16개 정수/8개 Long 규칙 (최대 복잡도)" : "16 int / 8 long rules with 3-5 op steps (Max).";
			case "tip_slogic_stealth" -> ko ? "4개 정수/2개 Long 규칙 (초경량 크기)" : "4 int / 2 long rules for minimal footprint.";

			// JNIC
			case "card_jnic" -> ko ? "JNIC / 네이티브 보호" : "JNIC / Native Protection";
			case "card_jnic_sub" -> ko ? "JNIC 네이티브 바이너리 컴파일 및 SLogic 이름 변경을 설정하세요." : "Configure JNIC native binary compilation and SLogic name obfuscation.";
			case "opt_jnic" -> ko ? "JNIC 난독화" : "JNIC Obfuscation";
			case "opt_jnic_sub" -> ko ? "C/C++ 네이티브 바이너리로 컴파일" : "Compile to C/C++ native binary";
			case "opt_slogic_rename" -> ko ? "SLogic 이름 변경" : "SLogic Name Change";
			case "opt_slogic_rename_sub" -> ko ? "SLogic 패키지 및 클래스 이름 난독화" : "Obfuscate SLogic class & package names";
			case "java_path" -> ko ? "자바 실행 경로" : "Java Path";
			case "jnic_path" -> ko ? "JNIC JAR 경로" : "JNIC JAR Path";
			case "jnic_xml_title" -> ko ? "JNIC XML 설정 (NecronOBF.xml)" : "JNIC Configuration XML (NecronOBF.xml)";
			case "btn_load_xml" -> ko ? "XML 불러오기" : "Load XML";
			case "btn_save_xml" -> ko ? "XML 저장" : "Save XML";
			case "btn_reset_xml" -> ko ? "기본값 복원" : "Default XML";

			// Bottom Buttons
			case "btn_import" -> ko ? "프로젝트 불러오기" : "Import Project";
			case "btn_save" -> ko ? "프로젝트 저장" : "Save Project";
			case "btn_export" -> ko ? "프로젝트 내보내기" : "Export Project";
			case "btn_protect" -> ko ? "보호 시작 \u2192" : "Protect \u2192";

			// Logs & Output
			case "output_title" -> ko ? "빌드 출력 및 로그" : "Build Output & Logs";

			default -> key;
		};
	}
}