package cuchaz.ships.gui;

import static cuchaz.ships.gui.GuiSettings.LeftMargin;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.inventory.Container;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL20;

import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BlockUtils.BlockConditionValidator;
import cuchaz.modsShared.BlockUtils.Neighbors;
import cuchaz.modsShared.ColorUtils;
import cuchaz.ships.MaterialProperties;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.Ships;
import cuchaz.ships.render.RenderShip2D;
import cuchaz.ships.render.ShaderLoader;

public class GuiShipPropulsion extends GuiShip
{
	private static final ResourceLocation DesaturationShader = new ResourceLocation( "ships", "/shaders/desaturate.frag" );
	
	private ShipLauncher m_shipLauncher;
	private BlockArray m_shipEnvelope;
	private BlockArray m_helmEnvelope;
	private BlockArray m_propulsionEnvelope;
	private int m_desaturationProgramId;
	
	public GuiShipPropulsion( Container container, final World world, int helmX, int helmY, int helmZ )
	{
		super( container );
		
		// defaults
		m_shipLauncher = null;
		m_shipEnvelope = null;
		m_helmEnvelope = null;
		m_propulsionEnvelope = null;
		m_desaturationProgramId = 0;
		
		// this should be the helm
		assert( world.getBlockId( helmX, helmY, helmZ ) == Ships.m_blockHelm.blockID );
		ChunkCoordinates helmCoords = new ChunkCoordinates( helmX, helmY, helmZ );
		
		// find the ship block
		ChunkCoordinates shipBlockCoords = BlockUtils.searchForBlock(
			helmX, helmY, helmZ,
			10000,
			new BlockConditionValidator( )
			{
				@Override
				public boolean isValid( ChunkCoordinates coords )
				{
					return !MaterialProperties.isSeparatorBlock( Block.blocksList[world.getBlockId( coords.posX, coords.posY, coords.posZ )] );
				}
				
				@Override
				public boolean isConditionMet( ChunkCoordinates coords )
				{
					return world.getBlockId( coords.posX, coords.posY, coords.posZ ) == Ships.m_blockShip.blockID;
				}
			},
			Neighbors.Edges
		);
		if( shipBlockCoords != null )
		{
			// get the ship definition and its top envelope
			m_shipLauncher = new ShipLauncher( world, shipBlockCoords.posX, shipBlockCoords.posY, shipBlockCoords.posZ );
			m_shipEnvelope = m_shipLauncher.getShipEnvelope( BlockSide.Top );
			
			// compute an envelope for the helm
			helmCoords.posX -= shipBlockCoords.posX;
			helmCoords.posY -= shipBlockCoords.posY;
			helmCoords.posZ -= shipBlockCoords.posZ;
			m_helmEnvelope = m_shipEnvelope.newEmptyCopy();
			m_helmEnvelope.setBlock( helmCoords.posX, helmCoords.posZ, helmCoords );
			
			// UNDONE: compute an envelope for the propulsion systems
			m_propulsionEnvelope = m_shipEnvelope.newEmptyCopy();
			
			// create our shader
			try
			{
				int shaderId = ShaderLoader.load( DesaturationShader );
				m_desaturationProgramId = GL20.glCreateProgram();
				GL20.glAttachShader( m_desaturationProgramId, shaderId );
				GL20.glLinkProgram( m_desaturationProgramId );
				GL20.glValidateProgram( m_desaturationProgramId );
			}
			catch( IOException ex )
			{
				// UNDONE: log the exception
				ex.printStackTrace();
			}
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		drawText( GuiString.ShipPropulsion.getLocalizedText(), 0 );
		
		if( m_shipLauncher != null )
		{
			int x = LeftMargin;
			int y = getLineY( 6 );
			int width = xSize - LeftMargin*2;
			int height = 64;
			
			RenderShip2D.drawWater( x, y, zLevel, width, height );
			
			if( m_shipEnvelope.getHeight() > m_shipEnvelope.getWidth() )
			{
				// rotate the envelopes so the long axis is across the GUI width
				m_shipEnvelope = BlockArray.Rotation.Ccw90.rotate( m_shipEnvelope );
				m_helmEnvelope = BlockArray.Rotation.Ccw90.rotate( m_helmEnvelope );
				m_propulsionEnvelope = BlockArray.Rotation.Ccw90.rotate( m_propulsionEnvelope );
			}
			
			// draw a desaturated ship
			RenderShip2D.drawShipAsColor(
				m_shipEnvelope,
				ColorUtils.getGrey( 64 ),
				x, y, zLevel, width, height
			);
			GL20.glUseProgram( m_desaturationProgramId );
			RenderShip2D.drawShip(
				m_shipEnvelope,
				m_shipLauncher.getShipWorld(),
				x, y, zLevel, width, height
			);
			GL20.glUseProgram( 0 );
			
			// draw the propulsion blocks and the helm at full saturation
			RenderShip2D.drawShip(
				m_propulsionEnvelope,
				m_shipLauncher.getShipWorld(),
				x, y, zLevel, width, height
			);
			RenderShip2D.drawShip(
				m_helmEnvelope,
				m_shipLauncher.getShipWorld(),
				x, y, zLevel, width, height
			);
		}
	}
}