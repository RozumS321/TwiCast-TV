package app.twicast.tv.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Sizes and spacing from DESIGN.md, in design units (1 unit = 1px of the 1280×720 mockup). */
object TwiDimens {
    val SidebarCollapsedWidth = 88.dp
    val SidebarExpandedWidth = 264.dp
    val SidebarHorizontalPadding = 18.dp
    val SidebarTopPadding = 28.dp
    val SidebarBottomPadding = 24.dp
    val SidebarItemHeight = 52.dp
    val SidebarItemSpacing = 6.dp
    val SidebarIndicatorWidth = 4.dp
    val SidebarIndicatorVerticalInset = 14.dp
    val SidebarEdgeShadowWidth = 24.dp
    val NavigationIconSize = 22.dp
    val NavigationLabelGap = 12.dp
    val NavigationItemRadius = 16.dp
    val SidebarLogoBottomSpacing = 20.dp
    val LogoMarkSize = 36.dp

    val ContentStartPadding = 40.dp
    val ContentEndPadding = 40.dp
    val ScreenTopPadding = 30.dp
    val SectionSpacing = 26.dp
    val SectionTitleSpacing = 14.dp

    val CardSpacing = 24.dp
    val CardContentSpacing = 10.dp
    val CardContentSpacingDense = 8.dp
    val CardTextHorizontalPadding = 4.dp
    val CardBottomPadding = 10.dp
    /** Room around a row so a 1.06× focused card and its shadow are not clipped. */
    val FocusOverflowPadding = 24.dp

    val StreamCardWidth = 240.dp
    val StreamCardDenseWidth = 212.dp
    /** 4-column grid on the category streams screen. */
    val StreamCardGridWidth = 258.dp
    /** 6-column grid on the Categories screen. */
    val CategoryCardWidth = 167.dp
    val CardTextSpacing = 2.dp
    val CategoryTextHorizontalPadding = 2.dp
    val RankInset = 8.dp
    val AvatarSmall = 32.dp
    val AvatarSmallDense = 30.dp
    val AvatarRingWidth = 2.dp
    val BadgeInset = 10.dp

    val FocusBorderWidth = 3.dp
    val FocusShadowElevation = 18.dp
    val OutlineWidth = 1.dp

    val ButtonHeight = 52.dp
    val ButtonHorizontalPadding = 26.dp
    val ButtonIconSize = 16.dp
    val ButtonIconGap = 10.dp
    val ButtonRestShadowElevation = 10.dp
    val ButtonShadowElevation = 12.dp
    val SecondaryButtonHorizontalPadding = 22.dp
    val ButtonSpacing = 12.dp
    val ChipSpacing = 10.dp
    val TagSpacing = 8.dp
    val IconButtonSize = 48.dp
    val IconButtonIconSize = 20.dp
    val ChipHeight = 40.dp
    val ChipHorizontalPadding = 18.dp
    val TagHorizontalPadding = 11.dp
    val TagVerticalPadding = 5.dp
    val TagDenseHorizontalPadding = 10.dp
    val TagDenseVerticalPadding = 4.dp
    val BadgeHorizontalPadding = 7.dp
    val BadgeVerticalPadding = 3.dp
    val BadgeLargeHorizontalPadding = 9.dp
    val BadgeLargeVerticalPadding = 4.dp
    val CountHorizontalPadding = 8.dp
    val CountLargeHorizontalPadding = 10.dp
    val RankVerticalPadding = 2.dp
    val SkeletonLineHeight = 14.dp
    val SkeletonCaptionHeight = 12.dp
    val SkeletonLineSpacing = 6.dp
    val MessageMaxWidth = 560.dp
    val PlayerOverlayPadding = 40.dp
    val PlayerPlateHorizontalPadding = 20.dp
    val PlayerPlateVerticalPadding = 14.dp
    val PlayerControlsInset = 40.dp
    val PlayerControlsHorizontalPadding = 24.dp
    val PlayerControlsVerticalPadding = 20.dp
    val PlayerControlsSpacing = 16.dp
    val PlayerAvatarSize = 48.dp
    val QualityPanelWidth = 400.dp
    val QualityPanelHorizontalPadding = 28.dp
    val QualityPanelVerticalPadding = 40.dp
    val QualityRowHeight = 52.dp
    val QualityRowPadding = 18.dp
    val QualityRowSpacing = 6.dp

    val GridFocusInset = 8.dp
    val GridTopPadding = 20.dp
    val GridVerticalSpacing = 22.dp
    val CategoryGridHorizontalSpacing = 20.dp
    val StreamGridHorizontalSpacing = 24.dp
    val CategoryHeaderSpacing = 22.dp
    val CategoryHeaderCoverWidth = 72.dp
    val CategoryHeaderCoverHeight = 96.dp

    val SearchPanelWidth = 392.dp
    val SearchColumnSpacing = 36.dp
    val SearchBlockSpacing = 16.dp
    val QueryFieldHeight = 60.dp
    val QueryFieldPadding = 18.dp
    val QueryFieldIconSize = 22.dp
    val QueryFieldRing = 2.dp
    val QueryCaretWidth = 2.dp
    val QueryCaretHeight = 26.dp
    val KeyHeight = 46.dp
    val KeySpacing = 8.dp
    val ActionKeyHeight = 48.dp
    val EraseKeyWidth = 64.dp
    val LayoutKeyWidth = 76.dp
    val ActionKeyIconSize = 20.dp
    val ChannelRowHeight = 68.dp
    val ChannelRowPadding = 16.dp
    val ChannelRowSpacing = 8.dp
    val ChannelRowGap = 14.dp
    val SearchAvatarSize = 44.dp
    val SearchCategoryWidth = 110.dp
    val SearchCategorySpacing = 18.dp
    val SearchSectionSpacing = 26.dp

    val UpdatePanelWidth = 880.dp
    val UpdatePanelHorizontalPadding = 48.dp
    val UpdatePanelVerticalPadding = 40.dp
    val UpdatePanelSpacing = 22.dp
    val UpdateHeaderSpacing = 18.dp
    val UpdateLogoSize = 56.dp
    val NotesCardSpacing = 18.dp
    val NotesCardHorizontalPadding = 20.dp
    val NotesCardVerticalPadding = 18.dp
    val NotesItemSpacing = 10.dp
    val NotesBadgeSize = 26.dp
    val NotesBadgeIconSize = 16.dp
    val ProgressHeight = 10.dp
    val BadgePillHorizontalPadding = 14.dp
    val BadgePillVerticalPadding = 8.dp
    val SkeletonTitleHeight = 22.dp
    val SkeletonGreetingWidth = 420.dp
    val SkeletonRowTitleWidth = 220.dp

    val HomeSectionSpacing = 22.dp
    val HomeRowTitleSpacing = 12.dp
    val HomeCardSpacing = 22.dp
    val HomeHeaderSpacing = 14.dp
    val RowHintSpacing = 10.dp

    val LoginPanelWidth = 960.dp
    val LoginPanelHorizontalPadding = 48.dp
    val LoginPanelVerticalPadding = 44.dp
    val LoginPanelSpacing = 48.dp
    val LoginPanelShadowElevation = 12.dp
    val LoginBlockSpacing = 22.dp
    val LoginHeaderSpacing = 8.dp
    val LoginStepSpacing = 14.dp
    val LoginStepBadgeSize = 32.dp
    val LoginStatusSpacing = 16.dp
    val CodeCellWidth = 48.dp
    val CodeCellHeight = 60.dp
    val CodeCellSpacing = 8.dp
    val CodeSeparatorPadding = 2.dp
    val StatusDotSize = 10.dp
    val StatusDotRing = 5.dp
    val StatusDotSpacing = 10.dp
    val QrSize = 264.dp
    val QrPadding = 18.dp
    val QrCaptionSpacing = 12.dp
    val QrShadowElevation = 12.dp
}

object TwiShapes {
    val Card = RoundedCornerShape(16.dp)
    val Thumbnail = RoundedCornerShape(14.dp)
    val Cover = RoundedCornerShape(14.dp)
    val Hero = RoundedCornerShape(20.dp)
    val Button = RoundedCornerShape(16.dp)
    val Chip = RoundedCornerShape(20.dp)
    val Badge = RoundedCornerShape(6.dp)
    val BadgeLarge = RoundedCornerShape(8.dp)
    val Panel = RoundedCornerShape(28.dp)
    val Pill = RoundedCornerShape(percent = 50)
    val SkeletonLine = RoundedCornerShape(6.dp)
    val CodeCell = RoundedCornerShape(12.dp)
    val Qr = RoundedCornerShape(22.dp)
    val QueryField = RoundedCornerShape(18.dp)
    val Key = RoundedCornerShape(12.dp)
    val NotesCard = RoundedCornerShape(18.dp)
}

/** Focus scales. Card scale 1.06 (not 1.08): at 1.08 neighbours in a row overlap. */
object TwiAspect {
    const val Thumbnail = 16f / 9f
    const val Cover = 3f / 4f
}

object TwiScale {
    const val CardFocused = 1.06f
    const val ButtonFocused = 1.05f
}
