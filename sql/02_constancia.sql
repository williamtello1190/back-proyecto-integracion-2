-- Script de actualización: agrega el área de Cobranza Coactiva (si no existe)
-- y la tabla Constancia, que registra cada transferencia de un expediente
-- desde Sancionador hacia Cobranza Coactiva.
--
-- Ejecutar sobre la base de datos GestionExpedientes ya creada.

USE GestionExpedientes;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Area WHERE nombre_area = 'Cobranza Coactiva')
BEGIN
    INSERT INTO dbo.Area (nombre_area, descripcion, estado)
    VALUES ('Cobranza Coactiva', 'Área encargada del proceso de cobranza coactiva', 'A');
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Constancia')
BEGIN
    CREATE TABLE dbo.Constancia (
        id_constancia        INT IDENTITY(1,1) PRIMARY KEY,
        id_expediente        INT NOT NULL REFERENCES dbo.Expediente(id_expediente),
        numero_constancia    VARCHAR(30)  NOT NULL UNIQUE,
        fecha_constancia     DATETIME2    NOT NULL,
        id_usuario_registro  INT NOT NULL REFERENCES dbo.Usuario(id_usuario),
        id_usuario_modifico  INT NULL     REFERENCES dbo.Usuario(id_usuario),
        fecha_registro       DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
        fecha_modificacion   DATETIME2    NULL,
        observacion          VARCHAR(500) NULL,
        estado               VARCHAR(20)  NOT NULL DEFAULT 'REGISTRADA'
    );

    CREATE INDEX IX_Constancia_Expediente ON dbo.Constancia(id_expediente);
END
GO

-- Recuerda: para que la "bandeja coactiva" del frontend funcione, los usuarios
-- de esa área deben tener su columna Usuario.id_area apuntando al área
-- 'Cobranza Coactiva' recién creada/existente, y los usuarios de Sancionador
-- deben apuntar al área 'Sancionador'.
