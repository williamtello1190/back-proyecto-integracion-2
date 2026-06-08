-- Script de actualización: agrega a Expediente los campos donde se guardará
-- lo extraído por OCR/IA (resumen generado y datos de la resolución/administrado),
-- usados para mostrar al usuario si el expediente es apto para derivar a Coactiva.
USE GestionExpedientes;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Expediente') AND name = 'resumen_ia')
BEGIN
    ALTER TABLE dbo.Expediente ADD resumen_ia NVARCHAR(1000) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Expediente') AND name = 'numero_resolucion')
BEGIN
    ALTER TABLE dbo.Expediente ADD numero_resolucion VARCHAR(60) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Expediente') AND name = 'administrado')
BEGIN
    ALTER TABLE dbo.Expediente ADD administrado VARCHAR(200) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Expediente') AND name = 'ruc_administrado')
BEGIN
    ALTER TABLE dbo.Expediente ADD ruc_administrado VARCHAR(20) NULL;
END
GO
