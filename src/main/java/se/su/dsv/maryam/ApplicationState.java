package se.su.dsv.maryam;

public final class ApplicationState {

	// States
	public static final int	MAIN_MAP_STATE				= 1;
	public static final int	STAR_SKY_STATE				= 2;

	// For MainMapFragment
	public static final int	NO_STARTING_POINT			= 1;
	public static final int	MAKE_READY					= 2;
	public static final int	IN_QUEST					= 3;
	public static final int	RETURN_TO_START				= 4;
	public static final int	OUTRO						= 5;
	public static final int	QUEST_COMPLETE				= 6;

	// For StarrySkyControllerFragment
	public static final int	SKY_NOT_OPEN				= 1;
	public static final int	SKY_OPEN_NOT_POSTED			= 2;
	public static final int	SKY_OPEN_HAS_POSTED			= 3;
	public static final int	SKY_OPEN_HAS_POSTED_TEMP	= 4;

	// For scene beeing played
	public static  boolean	SCENE_IS_PLAYING			=  false;

	// Debug or not
	public static boolean	D							= false;
}
