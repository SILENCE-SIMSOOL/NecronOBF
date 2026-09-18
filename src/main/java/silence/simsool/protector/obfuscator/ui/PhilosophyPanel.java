package silence.simsool.protector.obfuscator.ui;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import silence.simsool.protector.obfuscator.ui.component.ModernCard;
import silence.simsool.protector.obfuscator.ui.component.VectorIcon;

public final class PhilosophyPanel extends JPanel {

	private final JPanel list = new JPanel();

	public PhilosophyPanel() {
		setLayout(new BorderLayout(0, 14));
		setBackground(UITheme.BG_MAIN);
		setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

		buildContent();
		I18n.addListener(this::buildContent);
	}

	private void buildContent() {
		removeAll();

		boolean ko = I18n.current() == I18n.Lang.KO;

		// Header
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setOpaque(false);

		JLabel title = new JLabel(ko ? "SilenceProtector의 궁극적인 목표 & 아키텍처" : "SilenceProtector Manifesto & Architecture");
		title.setForeground(UITheme.TEXT_PRIMARY);
		title.setFont(UITheme.FONT_TITLE);

		JLabel sub = new JLabel(ko ?
			"리버싱 방지가 아닌 코드 재사용 방해를 통한 제로 오버헤드 Java 바이트코드 보호" :
			"Disrupting code reuse rather than attempting impossible reversing prevention — Zero-overhead Java protection."
		);
		sub.setForeground(UITheme.TEXT_SECONDARY);
		sub.setFont(UITheme.FONT_SUB);

		header.add(title);
		header.add(Box.createVerticalStrut(4));
		header.add(sub);
		add(header, BorderLayout.NORTH);

		// Content List in ScrollPane
		list.removeAll();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setOpaque(false);

		// 0. 우리의 궁극적인 목표 (사용자 추가 요청 사항)
		list.add(createSectionCard(
			VectorIcon.Type.SHIELD,
			ko ? "우리의 궁극적인 목표 (The Ultimate Goal)" : "The Ultimate Goal: Anti-Reuse Paradigm",
			ko ?
			"리버싱을 완전히 막는 것은 기술적으로 불가능합니다.\n" +
			"리버싱을 막기 위해 무거운 VM 가상화나 과도한 기법들을 사용하면 런타임 성능이 심각하게 훼손되지만, 그럼에도 결국 완전한 방어는 할 수 없습니다.\n\n" +
			"그래서 SilenceProtector는 완전한 소스코드 은닉을 좇기보다는 '코드 재사용을 극도로 어렵게 만드는 데' 목표를 둡니다.\n" +
			"분석자가 디컴파일을 시도하더라도 원래의 코드 구조와 개발 의도가 완전히 파괴되어 다른 곳에 재사용하거나 수정할 가치가 없게 만들며, 성능 손실은 '제로에 가깝게' 유지합니다." :
			"Completely preventing reverse engineering is technically impossible on the JVM.\n" +
			"Forcing heavy VM virtualization or excessive obfuscation severely damages runtime performance, yet still fails to offer absolute protection.\n\n" +
			"SilenceProtector therefore focuses on making 'code reuse' prohibitively difficult rather than pursuing impossible perfect source hiding.\n" +
			"Even if an attacker decompiles the bytecode, the original structure and developer intent are irreversibly broken, making stolen code unusable while keeping performance loss near zero."
		));
		list.add(Box.createVerticalStrut(12));

		// 1. 추천 가이드 (사용자 추가 요청 사항)
		list.add(createSectionCard(
			VectorIcon.Type.CODE,
			ko ? "추천 가이드 (Recommended Architecture)" : "Recommended Architecture & Usage Guide",
			ko ?
			"\u2022 다른 난독화기와 레이어로 사용하기 좋습니다:\n" +
			"  SilenceProtector는 매우 가볍고 빠르므로 다른 고수준 난독화기와 함께 1차 또는 2차 파이프라인 레이어로 조합하여 사용하기에 이상적입니다.\n\n" +
			"\u2022 SLogic 클래스의 JNI 난독화 권장:\n" +
			"  SLogic 클래스에는 프로젝트의 모든 복호화 로직이 담겨있기 때문에, 이 클래스를 JNI(C/C++ 네이티브) 난독화를 진행하는 것이 좋습니다. Java 바이트코드 분석만으로 복호화 알고리즘을 확보하는 것을 강력하게 차단합니다.\n\n" +
			"\u2022 패브릭(Fabric) 모드 완벽 자동 지원:\n" +
			"  패브릭 모드들을 자동으로 지원하기 때문에 따로 클래스를 설정하지 않으셔도 이름 난독화는 의존되는 함수 이름을 바꾸지 않아 추가적인 설정 없이 잘 돌아갑니다." :
			"\u2022 Excellent for Multi-Layer Obfuscation:\n" +
			"  Lightweight and fast, designed to be used seamlessly as a base or companion pipeline layer with secondary obfuscators.\n\n" +
			"\u2022 Recommended JNI Native Protection for SLogic:\n" +
			"  All decryption logic is centralized in the SLogic class. Applying JNI native compilation to SLogic completely shields decryption routines from bytecode decompilers.\n\n" +
			"\u2022 Native Fabric Mod Ecosystem Support:\n" +
			"  Out-of-the-box Fabric mod support automatically detects entrypoints and preserves required external contracts without tedious manual configuration."
		));
		list.add(Box.createVerticalStrut(12));

		// 2. 원본 소스 구조의 비가역적 파괴
		list.add(createSectionCard(
			VectorIcon.Type.DOCUMENT,
			ko ? "1. 원본 소스 구조의 비가역적 파괴" : "1. Irreversible Dismantling of Source Structure",
			ko ?
			"컴파일된 프로그램은 실행에 필요한 정보만 유지하면 됩니다. 원본 소스 코드에 존재했던 다음 정보들은 실행에 필요하지 않습니다:\n\n" +
			"- 원래 클래스와 패키지 구조\n" +
			"- 메서드와 필드 이름 및 지역 변수 정보\n" +
			"- 메서드 분리 기준과 개발자의 코드 작성 스타일\n" +
			"- 문자열과 숫자 상수의 원래 표현 방식\n" +
			"- 기능별 클래스 구성 및 논리적 배치\n\n" +
			"SilenceProtector는 이러한 정보를 최대한 제거하고 재구성하여, 프로그램 동작을 이해하더라도 원본 프로젝트처럼 유지보수 가능한 코드로 복원하는 것을 원천 봉쇄합니다." :
			"Compiled programs only need execution essentials. Source layout, package trees, variable identifiers, stylistic groupings, and literal constants are discarded and irreversibly shuffled, preventing reconstruction into maintainable code."
		));
		list.add(Box.createVerticalStrut(12));

		// 3. 리버싱 방지가 아닌 재사용 방해
		list.add(createSectionCard(
			VectorIcon.Type.LOCK,
			ko ? "2. 리버싱 방지가 아닌 재사용 방해" : "2. Anti-Reuse over Anti-Reversing",
			ko ?
			"SilenceProtector는 '절대로 분석할 수 없는 프로그램'을 목표로 하지 않습니다.\n" +
			"Java 프로그램은 결국 JVM에서 실행되어야 하므로 충분한 시간과 권한이 있다면 내부 동작을 분석할 수 있습니다.\n\n" +
			"> 코드를 분석하는 것은 가능하지만, 분석한 결과를 그대로 가져가서 원본 프로젝트처럼 개발을 이어가기 어렵게 만든다.\n\n" +
			"공격자가 특정 기능의 작동 원리를 확인하는 것과, 전체 프로젝트를 복구해서 자신의 코드처럼 사용하는 것을 전혀 다른 문제로 분리시킵니다." :
			"We do not claim 'unbreakable' code. Rather, analyzing bytecode and reusing it in another project are two completely different problems. Decompiled code is rendered too costly and fragmented to adapt."
		));
		list.add(Box.createVerticalStrut(12));

		// 4. 성능 손실 최소화 (Zero-Overhead)
		list.add(createSectionCard(
			VectorIcon.Type.SLIDERS,
			ko ? "3. 성능 손실 최소화 (Zero-Overhead)" : "3. Zero-Overhead Performance",
			ko ?
			"강력한 난독화라고 해서 실행 성능을 크게 희생해서는 안 됩니다.\n" +
			"런타임 비용이 발생하는 무거운 기법 대신, '빌드 시점에 구조를 파괴하는 방식'을 우선합니다.\n\n" +
			"\u2714 적극 활용: 클래스/패키지 재배치, 이름 제거, 비트 단위 경량 상수 변환, 문자열 1회 복호화 후 캐싱\n" +
			"\u2716 지양: 전 메서드 VM 가상화, 과도한 Reflection, 대규모 제어흐름 평탄화(Flattening), 반복적 런타임 복호화\n\n" +
			"궁극적으로 난독화를 하지 않은 순수 원본 프로그램과 최대한 비슷한 성능을 유지합니다." :
			"Performance is non-negotiable. Instead of heavy runtime virtual machines and reflection loops, we dismantle structures at build time. String decryption is cached once, keeping performance indistinguishable from clean code."
		));
		list.add(Box.createVerticalStrut(12));

		// 5. 매 빌드마다 다른 결과 (Polymorphic Dynamic Logic)
		list.add(createSectionCard(
			VectorIcon.Type.SHUFFLE,
			ko ? "4. 매 빌드마다 다른 결과 (Polymorphic Dynamic Logic)" : "4. Polymorphic Dynamic Logic per Build",
			ko ?
			"동일한 원본 JAR을 여러 번 난독화하더라도 매 빌드마다 완전히 다른 결과물이 생성됩니다:\n\n" +
			"- 랜덤 클래스 및 패키지 구조\n" +
			"- 랜덤 문자열 키와 바이트 오프셋\n" +
			"- 랜덤 숫자 연산 체인 (XOR, ADD, SUB, ROTL, ROTR, REVERSE) 및 내부 상수\n" +
			"- 매 빌드마다 새로 컴파일되는 SLogic.class 바이트코드\n\n" +
			"이를 통해 한 버전을 장시간 분석했더라도, 다음 배포본에는 이전 분석 결과나 시그니처를 전혀 재사용할 수 없습니다." :
			"Every build re-assembles SLogic bytecode, bit rotation parameters, arithmetic sequences, and random package structures. A signature reverse-engineered from release A is completely useless against release B."
		));
		list.add(Box.createVerticalStrut(12));

		// 6. 복호화 로직의 중앙화와 보호
		list.add(createSectionCard(
			VectorIcon.Type.CUBE,
			ko ? "5. 복호화 로직의 중앙화와 보호" : "5. Centralized Decryption & Protection",
			ko ?
			"문자열 및 숫자 복호화 로직은 SLogic 클래스에 집중됩니다.\n" +
			"일반 비즈니스 클래스는 암호화된 값만 보관하며, 필요한 순간 SLogic을 통해 원본 값을 얻습니다.\n" +
			"문자열은 최초 1회 복호화 후 클래스별 합성 캐시 필드($sp$c)에 보관되므로 반복 호출에 따른 성능 저하가 없습니다." :
			"Decryption routines are concentrated in SLogic. Business classes store only scrambled data and invoke SLogic. Strings are cached in synthetic static arrays upon first call, eliminating recurring decryption costs."
		));
		list.add(Box.createVerticalStrut(12));

		// 7. 디컴파일 가능한 코드와 유지보수 가능한 코드는 다르다
		list.add(createSectionCard(
			VectorIcon.Type.NOTE,
			ko ? "6. 디컴파일 가능한 코드와 유지보수 가능한 코드는 다르다" : "6. Decompilable vs Maintainable Code",
			ko ?
			"목표는 단순히 디컴파일러에서 에러를 띄우는 것이 아닙니다.\n" +
			"디컴파일러가 Java 코드를 정상 출력하더라도 다음 상태를 만드는 것이 핵심입니다:\n\n" +
			"실행 로직은 온전히 존재함 \u2192 기능 분석은 시도 가능 \u2192 그러나 원래 구조와 이름은 완전 소멸 \u2192 " +
			"상수와 문자열은 비트 연산으로 은닉 \u2192 메서드의 원래 역할을 일일이 재추론해야 함 \u2192 " +
			"코드를 가져가서 수정하거나 새 기능을 추가하기 불가능\n\n" +
			"'디컴파일 실패'보다 '디컴파일 후에도 쓸모없는 코드'를 만드는 것이 SilenceProtector의 본질입니다." :
			"Causing decompiler crashes is trivial and easily patched. The real goal is emitting valid Java code where every identifier, relationship, and literal is so dismantled that reusing or modifying it is costlier than rebuilding from scratch."
		));
		list.add(Box.createVerticalStrut(12));

		// 8. 핵심 원칙 요약
		list.add(createSectionCard(
			VectorIcon.Type.BOOK,
			ko ? "7. 최종 목표 & 핵심 원칙" : "7. Final Objective & Core Creed",
			ko ?
			"> \"가볍게 실행되고, 강하게 구조를 파괴하며, 분석 후에도 원본 프로젝트로 되돌리기 어렵게 만든다.\"\n\n" +
			"SilenceProtector는 단순한 이름 난독화 도구가 아닙니다.\n" +
			"바이트코드의 구조적 정보를 최대한 제거하고 코드 재사용을 불가능하게 만드는 현대적인 고성능 Java 보호 솔루션입니다." :
			"> \"Execute lightly, dismantle structures irreversibly, and render stolen code impossible to repurpose.\"\n\n" +
			"SilenceProtector is not a simple renamer — it is a zero-overhead Java structural defense engine."
		));

		JScrollPane scroll = new JScrollPane(list);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);

		revalidate();
		repaint();
	}

	private ModernCard createSectionCard(VectorIcon.Type iconType, String title, String body) {
		ModernCard card = new ModernCard(iconType, title, null);
		JTextArea text = new JTextArea(body);
		text.setEditable(false);
		text.setWrapStyleWord(true);
		text.setLineWrap(true);
		text.setOpaque(false);
		text.setForeground(UITheme.TEXT_SECONDARY);
		text.setFont(UITheme.FONT_SUB);
		text.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));
		card.add(text, BorderLayout.CENTER);
		return card;
	}
}